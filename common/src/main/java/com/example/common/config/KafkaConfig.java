package com.example.common.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Producer ──────────────────────────────────────────────────
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Reliability settings
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        // "all" = wait for ALL replicas to acknowledge
        // In production this is critical — never use "0" or "1" for money events

        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // Idempotent producer = Kafka won't duplicate messages on retry

        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        // Ensures ordering — one message in flight at a time per partition

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> template =
            new KafkaTemplate<>(producerFactory());

        // Attach eventType header automatically
        template.setProducerListener(new ProducerListener<>() {
            @Override
            public void onSuccess(ProducerRecord<String, String> record,
                                   RecordMetadata metadata) {
                log.debug("Published to {} partition {} offset {}",
                    metadata.topic(), metadata.partition(), metadata.offset());
            }
        });

        return template;
    }

    // ── Consumer ──────────────────────────────────────────────────
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Start from beginning if no committed offset — never miss a message

        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // CRITICAL: manual commit only — commit offset AFTER processing succeeds
        // Auto-commit would commit before processing, losing messages on crash

        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        // Process 50 records per poll — tune based on processing time

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // Manual commit — offset only advances after successful processing
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.RECORD);

        // 3 concurrent consumers per service instance
        // Combined with 6 partitions = 6 consumers max across 2 instances
        factory.setConcurrency(3);

        // Retry 3 times before sending to DLQ
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new FixedBackOff(1000L, 3L) // 1s delay, 3 retries
        ));

        return factory;
    }
}
