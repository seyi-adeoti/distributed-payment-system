package com.example.wallet.saga;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.event.DebitReversalRequestedEvent;
import com.example.common.event.DebitReversedEvent;
import com.example.common.event.PaymentInitiatedEvent;
import com.example.common.event.WalletDebitFailedEvent;
import com.example.common.event.WalletDebitedEvent;
import com.example.common.exception.InsufficientFundsException;
import com.example.common.exception.KycLimitExceededException;
import com.example.common.exception.WalletInactiveException;
import com.example.wallet.entity.ProcessedEvent;
import com.example.wallet.repository.ProcessedEventRepository;
import com.example.wallet.service.OutboxService;
import com.example.wallet.service.WalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletSagaParticipant {

    private final WalletService walletService;
    private final OutboxService outboxService;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void handlePaymentInitiated(PaymentInitiatedEvent event) {

        // Idempotency — if we've seen this reference before, skip
        if (isAlreadyProcessed(event.getReference())) {
            log.warn("Already processed payment {}. Skipping debit.", event.getReference());
            return;
        }

        log.info("Processing debit for payment: {}", event.getPaymentId());

        try {
            // Debit sender
            walletService.debitForPayment(
                    event.getSenderWalletId(),
                    event.getAmount(),
                    event.getReference(),
                    event.getNarration());

            // Credit receiver
            walletService.creditForPayment(
                    event.getReceiverWalletId(),
                    event.getAmount(),
                    event.getReference(),
                    event.getNarration());

            // Get updated balances for the event
            BigDecimal senderBalance = walletService
                    .getBalance(event.getSenderWalletId());
            BigDecimal receiverBalance = walletService
                    .getBalance(event.getReceiverWalletId());

            markProcessed(event.getReference(), "PaymentInitiated");

            // Publish success
            outboxService.write(
                    event.getPaymentId(),
                    "Wallet",
                    "WalletDebited",
                    WalletDebitedEvent.builder()
                            .paymentId(event.getPaymentId())
                            .senderWalletId(event.getSenderWalletId())
                            .receiverWalletId(event.getReceiverWalletId())
                            .amount(event.getAmount())
                            .reference(event.getReference())
                            .senderBalanceAfter(senderBalance)
                            .receiverBalanceAfter(receiverBalance)
                            .occurredAt(LocalDateTime.now())
                            .build());

            log.info("Wallet debit+credit complete for payment: {}", event.getPaymentId());

        } catch (InsufficientFundsException e) {
            handleDebitFailure(event, "INSUFFICIENT_FUNDS: " + e.getMessage());
        } catch (WalletInactiveException e) {
            handleDebitFailure(event, "WALLET_INACTIVE: " + e.getMessage());
        } catch (KycLimitExceededException e) {
            handleDebitFailure(event, "LIMIT_EXCEEDED: " + e.getMessage());
        }
    }

    @Transactional
    public void handleDebitReversal(DebitReversalRequestedEvent event) {

        String reversalRef = event.getReversalReference();

        if (isAlreadyProcessed(reversalRef)) {
            log.warn("Already reversed {}. Skipping.", reversalRef);
            return;
        }

        log.info("Reversing debit for payment: {}", event.getPaymentId());

        // Reverse: credit sender (give money back)
        walletService.creditForPayment(
                event.getSenderWalletId(),
                event.getAmount(),
                reversalRef,
                "Reversal: " + event.getReason());

        // Reverse: debit receiver (take money back — they never should have had it)
        walletService.debitForReversal(
                event.getReceiverWalletId(),
                event.getAmount(),
                reversalRef + "-RCV",
                "Reversal: " + event.getReason());

        markProcessed(reversalRef, "DebitReversalRequested");

        outboxService.write(
                event.getPaymentId(),
                "Wallet",
                "DebitReversed",
                DebitReversedEvent.builder()
                        .paymentId(event.getPaymentId())
                        .originalReference(event.getOriginalReference())
                        .reversalReference(reversalRef)
                        .occurredAt(LocalDateTime.now())
                        .build());

        log.info("Debit reversal complete for payment: {}", event.getPaymentId());
    }

    private void handleDebitFailure(PaymentInitiatedEvent event, String reason) {
        markProcessed(event.getReference(), "PaymentInitiated");

        outboxService.write(
                event.getPaymentId(),
                "Wallet",
                "WalletDebitFailed",
                WalletDebitFailedEvent.builder()
                        .paymentId(event.getPaymentId())
                        .reference(event.getReference())
                        .reason(reason)
                        .occurredAt(LocalDateTime.now())
                        .build());

        log.warn("Wallet debit failed. Payment: {} Reason: {}",
                event.getPaymentId(), reason);
    }

    private boolean isAlreadyProcessed(String reference) {
        return processedEventRepository.existsByReference(reference);
    }

    private void markProcessed(String reference, String eventType) {
        processedEventRepository.save(ProcessedEvent.builder()
                .reference(reference)
                .eventType(eventType)
                .processedAt(LocalDateTime.now())
                .build());
    }
}
