package com.example.payment.publisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.common.KafkaTopics;
import com.example.payment.entity.OutboxEvent;
import com.example.payment.entity.OutboxStatus;
import com.example.payment.repository.OutboxRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int MAX_RETRY = 5;

    @Scheduled(fixedDelay = 500) // every 500ms
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEvent> pending = outboxRepository
            .findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : pending) {
            try {
                String topic = resolveTopic(event.getEventType());

                // Partition key = aggregateId
                // All events for same payment go to same partition = ordered
                kafkaTemplate.send(topic,
                    event.getAggregateId().toString(),
                    event.getPayload())
                .get(5, TimeUnit.SECONDS); // wait for broker 

                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                outboxRepository.save(event);

                log.debug("Published: {} → {}", event.getEventType(), topic);

            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage());

                if (event.getRetryCount() >= MAX_RETRY) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event permanently failed after {} retries. Id: {}",
                        MAX_RETRY, event.getId());
                    // Alert goes here — this needs human attention
                }

                outboxRepository.save(event);
            }
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "PaymentInitiated",
                 "PaymentCompleted",
                 "PaymentFailed"       -> KafkaTopics.PAYMENT_EVENTS;
            case "DebitReversalRequested" -> KafkaTopics.COMPENSATION_EVENTS;
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}