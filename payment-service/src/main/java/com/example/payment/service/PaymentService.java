package com.example.payment.service;

import com.example.common.event.PaymentInitiatedEvent;

import com.example.payment.entity.OutboxEvent;
import com.example.payment.entity.OutboxStatus;
import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.repository.OutboxRepository;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.response.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional  // THIS IS THE KEY — both writes in one transaction
    public PaymentResponse initiatePayment(PaymentInitiatedEvent request) {

        // log.info(request);

        // Idempotency check
        if (paymentRepository.existsByReference(request.getReference())) {
            return mapToResponse(
                paymentRepository.findByReference(request.getReference()).get()
            );
        }

        // 1. Create payment record
        Payment payment = Payment.builder()
                .senderWalletId(request.getSenderWalletId())
                .receiverWalletId(request.getReceiverWalletId())
                .amount(request.getAmount())
                .currency("NGN")
                .reference(request.getReference())
                .narration(request.getNarration())
                .status(PaymentStatus.INITIATED)
                .build();

        payment = paymentRepository.save(payment);

        // 2. Write event to outbox — SAME transaction
        // If DB commit fails, BOTH the payment and the outbox row roll back
        // If DB commit succeeds, BOTH exist — the event cannot be lost
        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .paymentId(payment.getId())
                .senderWalletId(payment.getSenderWalletId())
                .receiverWalletId(payment.getReceiverWalletId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .reference(payment.getReference())
                .narration(payment.getNarration())
                .occurredAt(LocalDateTime.now())
                .build();

        OutboxEvent outbox = OutboxEvent.builder()
                .aggregateId(payment.getId())
                .aggregateType("Payment")
                .eventType("PaymentInitiated")
                .payload(toJson(event))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxRepository.save(outbox);

        log.info("Payment initiated. Ref: {} OutboxEvent written.", payment.getReference());

        return mapToResponse(payment);
    }

    // Called by Saga when downstream confirms success
    @Transactional
    public void markCompleted(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Publish completion event via outbox
        writeOutbox(paymentId, "Payment", "PaymentCompleted",
            PaymentCompletedEvent.builder()
                .paymentId(paymentId)
                .reference(payment.getReference())
                .amount(payment.getAmount())
                .occurredAt(LocalDateTime.now())
                .build());

        log.info("Payment completed. Id: {}", paymentId);
    }

    @Transactional
    public void markFailed(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        writeOutbox(paymentId, "Payment", "PaymentFailed",
            PaymentFailedEvent.builder()
                .paymentId(paymentId)
                .reference(payment.getReference())
                .reason(reason)
                .occurredAt(LocalDateTime.now())
                .build());

        log.warn("Payment failed. Id: {} Reason: {}", paymentId, reason);
    }

    private void writeOutbox(UUID aggregateId, String aggregateType,
                              String eventType, Object event) {
        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventType(eventType)
                .payload(toJson(event))
                .status(OutboxStatus.PENDING)
                .build());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}