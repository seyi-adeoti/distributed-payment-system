package com.example.notification.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserServiceClient {

    // Dummy method to simulate fetching user data
    public String getUserEmail(UUID walletId) {
        log.info("Fetching user email for wallet: {}", walletId);
        return "user-" + walletId.toString().substring(0, 4) + "@example.com";
    }
}
