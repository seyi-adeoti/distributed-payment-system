package com.example.notification.service;

import org.springframework.stereotype.Service;

import com.example.notification.dto.NotificationRequest;
import com.example.notification.client.UserServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSender {

    private final UserServiceClient userServiceClient;

    public void send(NotificationRequest request) {
        String recipient = request.getWalletId() != null 
            ? userServiceClient.getUserEmail(request.getWalletId()) 
            : "unknown (payment: " + request.getPaymentId() + ")";
            
        log.info(">>> SENDING NOTIFICATION to {} <<<", recipient);
        log.info("Title: {}", request.getTitle());
        log.info("Message: {}", request.getMessage());
        log.info("Type: {}", request.getType());
        log.info("------------------------------------------------");
    }
}
