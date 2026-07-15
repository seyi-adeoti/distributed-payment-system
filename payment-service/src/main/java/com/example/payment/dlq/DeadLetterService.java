package com.example.payment.dlq;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterService {

    private final DeadLetterRepository deadLetterRepository;

    public void record(ConsumerRecord<String, String> record,
                       String eventType, Exception error) {

        DeadLetterEvent dlq = DeadLetterEvent.builder()
                .topic(record.topic())
                .partition(record.partition())
                .kafkaOffset(record.offset())
                .eventType(eventType)
                .payload(record.value())
                .errorMessage(error.getMessage())
                .status("UNRESOLVED")
                .build();

        deadLetterRepository.save(dlq);

        log.error("Event moved to DLQ. Topic:{} Offset:{} Type:{} Error:{}",
            record.topic(), record.offset(), eventType, error.getMessage());

        // In production: alert Slack/PagerDuty here
        // A DLQ message means a human needs to investigate
    }

    // Admin endpoint to retry DLQ events
    @Transactional
    public void retryEvent(UUID dlqEventId, KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterEvent event = deadLetterRepository.findById(dlqEventId)
                .orElseThrow(() -> new RuntimeException("DLQ event not found"));

        try {
            kafkaTemplate.send(event.getTopic(), event.getPayload()).get();
            event.setStatus("RESOLVED");
            deadLetterRepository.save(event);
            log.info("DLQ event retried successfully: {}", dlqEventId);
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            deadLetterRepository.save(event);
            throw new RuntimeException("Retry failed: " + e.getMessage());
        }
    }
}
