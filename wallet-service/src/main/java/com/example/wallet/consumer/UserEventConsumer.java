package com.example.wallet.consumer;

import java.math.BigDecimal;
// import java.time.LocalDateTime;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.common.KafkaTopics;
import com.example.common.event.UserCreatedEvent;
import com.example.common.event.UserUpgradedEvent;
import com.example.wallet.entity.Wallet;
import com.example.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "wallet-user-events-group")
    @Transactional
    public void consumeUserEvents(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            // Since our outbox publishes stringified JSON, we can inspect fields or use wrapper
            // But wait, the outbox publisher sends the event Payload directly.
            // Let's determine which event it is by checking for fields unique to each event
            
            if (root.has("firstName") && root.has("email")) {
                UserCreatedEvent event = objectMapper.treeToValue(root, UserCreatedEvent.class);
                handleUserCreated(event);
            } else if (root.has("newTier")) {
                UserUpgradedEvent event = objectMapper.treeToValue(root, UserUpgradedEvent.class);
                handleUserUpgraded(event);
            } else {
                log.warn("Unknown user event format: {}", message);
            }

        } catch (Exception e) {
            log.error("Failed to process user event: {}", message, e);
        }
    }

    private void handleUserCreated(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent for user: {}", event.getUserId());
        
        // Create default NGN wallet with TIER_0
        Wallet wallet = Wallet.builder()
            .userId(event.getUserId())
            .currency("NGN")
            .balance(BigDecimal.ZERO)
            .kycTier("TIER_0")
            .build();
            
        walletRepository.save(wallet);
        log.info("Default wallet created for user: {}", event.getUserId());
    }

    private void handleUserUpgraded(UserUpgradedEvent event) {
        log.info("Received UserUpgradedEvent for user: {} to tier: {}", event.getUserId(), event.getNewTier());
        
        // Update wallet for this user to the new tier
        walletRepository.findByUserId(event.getUserId()).ifPresent(wallet -> {
            wallet.setKycTier(event.getNewTier());
            walletRepository.save(wallet);
        });
        log.info("Wallets updated for user: {}", event.getUserId());
    }
}
