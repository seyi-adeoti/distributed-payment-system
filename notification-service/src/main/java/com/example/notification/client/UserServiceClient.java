package com.example.notification.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserServiceClient {

    // Dummy method to simulate fetching user data
    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUserEmail")
    public String getUserEmail(UUID walletId) {
        log.info("Fetching user email for wallet: {}", walletId);
        // Simulate a tiny delay that might randomly spike in a real scenario
        return "user-" + walletId.toString().substring(0, 4) + "@example.com";
    }

    public String fallbackGetUserEmail(UUID walletId, Throwable t) {
        log.warn("UserService circuit breaker triggered! Fallback email used for wallet: {}. Error: {}", walletId, t.getMessage());
        return "support@distributed-platform.com";
    }
}
