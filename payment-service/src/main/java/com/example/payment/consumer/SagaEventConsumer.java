package com.example.payment.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.common.KafkaTopics;
import com.example.common.event.DebitReversedEvent;
import com.example.common.event.LedgerPostFailedEvent;
import com.example.common.event.LedgerPostedEvent;
import com.example.common.event.WalletDebitFailedEvent;
import com.example.common.event.WalletDebitedEvent;
import com.example.payment.dlq.DeadLetterService;
import com.example.payment.saga.PaymentSagaOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventConsumer {

    private final PaymentSagaOrchestrator sagaOrchestrator;
    private final ObjectMapper objectMapper;
    private final DeadLetterService deadLetterService;

    // Consumes wallet outcomes
    @KafkaListener(
        topics = KafkaTopics.WALLET_EVENTS,
        groupId = "payment-service-saga",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onWalletEvent(ConsumerRecord<String, String> record) {
        String eventType = extractEventType(record.headers());
        log.info("Payment saga received wallet event: {}", eventType);

        try {
            switch (eventType) {
                case "WalletDebited" -> sagaOrchestrator.onWalletDebited(
                    deserialize(record.value(), WalletDebitedEvent.class));

                case "WalletDebitFailed" -> sagaOrchestrator.onWalletDebitFailed(
                    deserialize(record.value(), WalletDebitFailedEvent.class));

                case "DebitReversed" -> sagaOrchestrator.onDebitReversed(
                    deserialize(record.value(), DebitReversedEvent.class));

                default -> log.debug("Ignored wallet event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed processing wallet event: {} Error: {}",
                eventType, e.getMessage());
            deadLetterService.record(record, eventType, e);
            // Do NOT rethrow — that would cause infinite retry on a poison message
            // The DLQ handler will deal with it
        }
    }

    // Consumes ledger outcomes
    @KafkaListener(
        topics = KafkaTopics.LEDGER_EVENTS,
        groupId = "payment-service-saga",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onLedgerEvent(ConsumerRecord<String, String> record) {
        String eventType = extractEventType(record.headers());

        try {
            switch (eventType) {
                case "LedgerPosted" -> sagaOrchestrator.onLedgerPosted(
                    deserialize(record.value(), LedgerPostedEvent.class));

                case "LedgerPostFailed" -> sagaOrchestrator.onLedgerPostFailed(
                    deserialize(record.value(), LedgerPostFailedEvent.class));

                default -> log.debug("Ignored ledger event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed processing ledger event: {}", e.getMessage());
            deadLetterService.record(record, eventType, e);
        }
    }

    private String extractEventType(Headers headers) {
        Header header = headers.lastHeader("eventType");
        return header != null ? new String(header.value()) : "UNKNOWN";
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}
