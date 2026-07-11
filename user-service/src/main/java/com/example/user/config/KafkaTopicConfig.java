package com.example.user.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static com.example.common.KafkaTopics.*;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(PAYMENT_EVENTS)
                .partitions(6)
                .build();
    }

    @Bean
    public NewTopic walletEventsTopic() {
        return TopicBuilder.name(WALLET_EVENTS)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic compensationEventsTopic() {
        return TopicBuilder.name(COMPENSATION_EVENTS)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
