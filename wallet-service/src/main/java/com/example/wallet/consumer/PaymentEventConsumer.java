package com.example.wallet.consumer;

import java.time.LocalDateTime;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.KafkaTopics;
import com.example.common.event.DebitReversalRequestedEvent;
import com.example.common.event.PaymentInitiatedEvent;
import com.example.common.event.WalletDebitFailedEvent;
import com.example.common.event.WalletDebitedEvent;
import com.example.common.exception.InsufficientFundsException;
import com.example.wallet.entity.ProcessedEvent;
import com.example.wallet.repository.ProcessedEventRepository;
import com.example.wallet.service.OutboxService;
import com.example.wallet.service.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
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
    void handlePaymentInitiated(ConsumerRecord<String, String> record) {

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

    /**
     * Extract the event type from the JSON payload.
     * Convention: every event JSON has an "eventType" field,
     * or we infer it from the payload structure.
     */
    private String extractEventType(ConsumerRecord<String, String> record) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            if (node.has("eventType")) {
                return node.get("eventType").asText();
            }
            // Fallback: check for distinctive fields
            if (node.has("senderWalletId") && node.has("receiverWalletId")) {
                return "PaymentInitiated";
            }
            return "Unknown";
        } catch (Exception e) {
            log.error("Failed to extract event type from record", e);
            return "Unknown";
        }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event: " + clazz.getSimpleName(), e);
        }
    }
}
