package com.example.payment.saga;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.event.DebitReversalRequestedEvent;
import com.example.common.event.DebitReversedEvent;
import com.example.common.event.LedgerPostFailedEvent;
import com.example.common.event.LedgerPostRequestedEvent;
import com.example.common.event.LedgerPostedEvent;
import com.example.common.event.PaymentCompletedEvent;
import com.example.common.event.PaymentFailedEvent;
import com.example.common.event.PaymentReversedEvent;
import com.example.common.event.WalletDebitFailedEvent;
import com.example.common.event.WalletDebitedEvent;
import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaOrchestrator {

    private final PaymentSagaRepository sagaRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxService outboxService;
    private final SagaEventLogger sagaEventLogger;

    // ── Called when payment is first created ──────────────────────
    @Transactional
    public void onPaymentInitiated(UUID paymentId, String reference) {
        PaymentSaga saga = PaymentSaga.builder()
                .paymentId(paymentId)
                .currentStep(SagaStep.DEBIT_PENDING)
                .build();

        sagaRepository.save(saga);
        sagaEventLogger.log(paymentId, "SAGA_STARTED", "Debit pending");
        log.info("Saga started for payment: {}", paymentId);
    }

    // ── Wallet debited successfully ────────────────────────────────
    @Transactional
    public void onWalletDebited(WalletDebitedEvent event) {
        PaymentSaga saga = loadSaga(event.getPaymentId());

        // Guard — ignore if saga is already in a terminal state
        // (this event might arrive twice due to Kafka at-least-once)
        if (isTerminal(saga.getCurrentStep())) {
            log.warn("Saga already in terminal state {}. Ignoring WalletDebited.",
                saga.getCurrentStep());
            return;
        }

        saga.setCurrentStep(SagaStep.LEDGER_PENDING);
        saga.setDebitCompletedAt(LocalDateTime.now());
        sagaRepository.save(saga);

        sagaEventLogger.log(event.getPaymentId(), "DEBIT_COMPLETED",
            "Wallet debited. Requesting ledger post.");

        // Tell Ledger Service to post GL entries
        // We do this via outbox — same transaction
        outboxService.write(
            event.getPaymentId(),
            "PaymentSaga",
            "LedgerPostRequested",
            LedgerPostRequestedEvent.builder()
                .paymentId(event.getPaymentId())
                .reference(event.getReference())
                .senderWalletId(event.getSenderWalletId())
                .receiverWalletId(event.getReceiverWalletId())
                .amount(event.getAmount())
                .occurredAt(LocalDateTime.now())
                .build()
        );

        log.info("Saga: debit complete, ledger post requested. Payment: {}",
            event.getPaymentId());
    }

    // ── Wallet debit FAILED ────────────────────────────────────────
    @Transactional
    public void onWalletDebitFailed(WalletDebitFailedEvent event) {
        PaymentSaga saga = loadSaga(event.getPaymentId());

        if (isTerminal(saga.getCurrentStep())) return;

        saga.setCurrentStep(SagaStep.FAILED);
        saga.setFailedAt(LocalDateTime.now());
        saga.setFailureReason(event.getReason());
        sagaRepository.save(saga);

        // Mark payment failed
        Payment payment = paymentRepository.findById(event.getPaymentId()).orElseThrow();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(event.getReason());
        paymentRepository.save(payment);

        // No compensation needed — wallet was never debited
        // Just publish PaymentFailed so Notification Service can alert user
        outboxService.write(
            event.getPaymentId(),
            "PaymentSaga",
            "PaymentFailed",
            PaymentFailedEvent.builder()
                .paymentId(event.getPaymentId())
                .reference(event.getReference())
                .reason(event.getReason())
                .compensationRequired(false)
                .occurredAt(LocalDateTime.now())
                .build()
        );

        sagaEventLogger.log(event.getPaymentId(), "SAGA_FAILED",
            "Debit failed: " + event.getReason());
        log.warn("Saga failed at debit step. Payment: {} Reason: {}",
            event.getPaymentId(), event.getReason());
    }

    // ── Ledger posted successfully ─────────────────────────────────
    @Transactional
    public void onLedgerPosted(LedgerPostedEvent event) {
        PaymentSaga saga = loadSaga(event.getPaymentId());

        if (isTerminal(saga.getCurrentStep())) return;

        saga.setCurrentStep(SagaStep.COMPLETED);
        saga.setLedgerPostedAt(LocalDateTime.now());
        saga.setCompletedAt(LocalDateTime.now());
        sagaRepository.save(saga);

        // Mark payment completed
        Payment payment = paymentRepository.findById(event.getPaymentId()).orElseThrow();
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        outboxService.write(
            event.getPaymentId(),
            "PaymentSaga",
            "PaymentCompleted",
            PaymentCompletedEvent.builder()
                .paymentId(event.getPaymentId())
                .reference(event.getReference())
                .amount(event.getAmount())
                .occurredAt(LocalDateTime.now())
                .build()
        );

        sagaEventLogger.log(event.getPaymentId(), "SAGA_COMPLETED", "Payment fully processed.");
        log.info("Saga COMPLETED. Payment: {}", event.getPaymentId());
    }

    // ── Ledger post FAILED — trigger compensation ──────────────────
    @Transactional
    public void onLedgerPostFailed(LedgerPostFailedEvent event) {
        PaymentSaga saga = loadSaga(event.getPaymentId());

        if (isTerminal(saga.getCurrentStep())) return;

        // Money moved (wallet debited) but accounting failed.
        // We MUST reverse the debit. This is the compensation.
        saga.setCurrentStep(SagaStep.COMPENSATION_PENDING);
        saga.setCompensationReason("Ledger post failed: " + event.getReason());
        sagaRepository.save(saga);

        Payment payment = paymentRepository.findById(event.getPaymentId()).orElseThrow();

        // Request debit reversal — Wallet Service listens on compensation.events
        outboxService.write(
            event.getPaymentId(),
            "PaymentSaga",
            "DebitReversalRequested",
            DebitReversalRequestedEvent.builder()
                .paymentId(event.getPaymentId())
                .senderWalletId(payment.getSenderWalletId())
                .amount(payment.getAmount())
                .originalReference(event.getReference())
                .reversalReference(event.getReference() + "-REV")
                .reason("Ledger post failed")
                .occurredAt(LocalDateTime.now())
                .build()
        );

        sagaEventLogger.log(event.getPaymentId(), "COMPENSATION_INITIATED",
            "Debit reversal requested due to ledger failure.");
        log.warn("Saga compensation initiated. Payment: {}", event.getPaymentId());
    }

    // ── Debit reversed (compensation complete) ─────────────────────
    @Transactional
    public void onDebitReversed(DebitReversedEvent event) {
        PaymentSaga saga = loadSaga(event.getPaymentId());

        saga.setCurrentStep(SagaStep.REVERSED);
        saga.setReversedAt(LocalDateTime.now());
        sagaRepository.save(saga);

        Payment payment = paymentRepository.findById(event.getPaymentId()).orElseThrow();
        payment.setStatus(PaymentStatus.FAILED); // or REVERSED
        payment.setFailureReason(saga.getCompensationReason());
        paymentRepository.save(payment);

        outboxService.write(
            event.getPaymentId(),
            "PaymentSaga",
            "PaymentReversed",
            PaymentReversedEvent.builder()
                .paymentId(event.getPaymentId())
                .reference(event.getOriginalReference())
                .reason(saga.getCompensationReason())
                .occurredAt(LocalDateTime.now())
                .build()
        );

        sagaEventLogger.log(event.getPaymentId(), "SAGA_REVERSED",
            "Compensation complete. Money returned to sender.");
        log.info("Saga REVERSED. Payment: {}", event.getPaymentId());
    }

    // ── Helpers ────────────────────────────────────────────────────
    private PaymentSaga loadSaga(UUID paymentId) {
        return sagaRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException(
                    "Saga not found for payment: " + paymentId));
    }

    private boolean isTerminal(SagaStep step) {
        return step == SagaStep.COMPLETED
            || step == SagaStep.FAILED
            || step == SagaStep.REVERSED;
    }
}
