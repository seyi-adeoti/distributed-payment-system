package com.example.wallet.consumer;

import org.springframework.stereotype.Component;


import com.example.common.KafkaTopics;
import com.example.wallet.entity.ProcessedEvent;
import com.example.wallet.repository.ProcessedEventRepository;
import com.example.wallet.service.OutboxService;
import com.example.wallet.service.WalletService;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final WalletService walletService;
    private final OutboxService outboxService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = KafkaTopics.PAYMENT_EVENTS,
        groupId = "wallet-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentEvent(ConsumerRecord<String, String> record) {

        String eventType = extractEventType(record);
        log.info("Wallet Service received: {} offset:{}", eventType, record.offset());

        switch (eventType) {
            case "PaymentInitiated" -> handlePaymentInitiated(record);
            default -> log.debug("Ignored event type: {}", eventType);
        }
    }

    @Transactional
    private void handlePaymentInitiated(ConsumerRecord<String, String> record) {

        PaymentInitiatedEvent event = deserialize(
            record.value(), PaymentInitiatedEvent.class
        );

        // ── IDEMPOTENCY ────────────────────────────────────────────
        // Kafka guarantees at-least-once delivery.
        // This event could arrive TWICE — network hiccup, consumer restart, etc.
        // We must be safe to process it twice without double-debiting.
        if (processedEventRepository.existsByReference(event.getReference())) {
            log.warn("Already processed reference {}. Skipping.", event.getReference());
            return;
        }

        try {
            walletService.debitForPayment(
                event.getSenderWalletId(),
                event.getAmount(),
                event.getReference(),
                event.getNarration()
            );

            // Mark as processed BEFORE publishing success event
            // (both in same transaction)
            markProcessed(event.getReference(), "PaymentInitiated");

            // Publish success via outbox — same transaction
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
                    .occurredAt(LocalDateTime.now())
                    .build()
            );

            log.info("Wallet debited for payment. Ref: {}", event.getReference());

        } catch (InsufficientFundsException e) {

            markProcessed(event.getReference(), "PaymentInitiated");

            outboxService.write(
                event.getPaymentId(),
                "Wallet",
                "WalletDebitFailed",
                WalletDebitFailedEvent.builder()
                    .paymentId(event.getPaymentId())
                    .reference(event.getReference())
                    .reason("Insufficient funds")
                    .occurredAt(LocalDateTime.now())
                    .build()
            );

            log.warn("Wallet debit failed — insufficient funds. Ref: {}", event.getReference());
        }
    }

    // Also listens for compensation events — reverse a debit
    @KafkaListener(
        topics = KafkaTopics.COMPENSATION_EVENTS,
        groupId = "wallet-service-compensation"
    )
    @Transactional
    public void onCompensationEvent(ConsumerRecord<String, String> record) {

        DebitReversalRequestedEvent event = deserialize(
            record.value(), DebitReversalRequestedEvent.class
        );

        if (processedEventRepository.existsByReference(event.getReference() + "-REV")) {
            log.warn("Already reversed {}. Skipping.", event.getReference());
            return;
        }

        walletService.reverseDebit(
            event.getSenderWalletId(),
            event.getAmount(),
            event.getReference()
        );

        markProcessed(event.getReference() + "-REV", "DebitReversalRequested");

        log.info("Debit reversed for ref: {}", event.getReference());
    }

    private void markProcessed(String reference, String eventType) {
        processedEventRepository.save(ProcessedEvent.builder()
                .reference(reference)
                .eventType(eventType)
                .processedAt(LocalDateTime.now())
                .build());
    }
}
