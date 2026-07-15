package com.example.user.service.implementation;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.common.event.UserUpgradedEvent;
import com.example.user.entity.OutboxEvent;
import com.example.user.entity.OutboxStatus;
import com.example.user.entity.User;
import com.example.user.repository.OutboxRepository;
import com.example.user.repository.UserRepository;
import com.example.user.service.abstraction.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KycService {

    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void upgradeUserToTier1(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("TIER_1".equals(user.getKycTier())) {
            throw new RuntimeException("User is already TIER_1");
        }

        user.setKycTier("TIER_1");
        userRepository.save(user);

        // Publish UserUpgradedEvent to outbox
        try {
            UserUpgradedEvent eventPayload = UserUpgradedEvent.builder()
                .userId(user.getId())
                .newTier(user.getKycTier())
                .build();
                
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(user.getId())
                .aggregateType("User")
                .eventType("UserUpgradedEvent")
                .payload(objectMapper.writeValueAsString(eventPayload))
                .status(OutboxStatus.PENDING)
                .build();
            outboxRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to outbox", e);
        }
    }
}
