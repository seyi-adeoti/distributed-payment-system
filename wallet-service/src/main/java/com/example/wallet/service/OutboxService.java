package com.example.wallet.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.wallet.entity.OutboxEvent;
import com.example.wallet.entity.OutboxStatus;
import com.example.wallet.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes events to the outbox table within the current transaction.
 * A separate publisher (OutboxPublisher) polls and sends them to Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void write(UUID aggregateId, String aggregateType,
            String eventType, Object event) {

        OutboxEvent outbox = OutboxEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventType(eventType)
                .payload(toJson(event))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxRepository.save(outbox);
        log.debug("Outbox event written: {} for aggregate {}", eventType, aggregateId);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
