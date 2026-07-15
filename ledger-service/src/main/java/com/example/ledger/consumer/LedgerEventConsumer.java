package com.example.ledger.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.common.KafkaTopics;
import com.example.common.event.LedgerPostRequestedEvent;
import com.example.ledger.saga.LedgerSagaParticipant;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventConsumer {

    private final LedgerSagaParticipant sagaParticipant;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = KafkaTopics.PAYMENT_EVENTS,
        groupId = "ledger-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentEvent(ConsumerRecord<String, String> record) {
        String eventType = extractEventType(record.headers());
        log.info("Ledger service received event: {}", eventType);

        try {
            if ("LedgerPostRequested".equals(eventType)) {
                sagaParticipant.handleLedgerPostRequested(
                    deserialize(record.value(), LedgerPostRequestedEvent.class));
            } else {
                log.debug("Ignored event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed processing event: {} Error: {}", eventType, e.getMessage());
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
