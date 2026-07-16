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
        String recipient = request.getEmail() != null 
            ? request.getEmail() 
            : (request.getWalletId() != null 
                ? userServiceClient.getUserEmail(request.getWalletId()) 
                : "unknown (payment: " + request.getPaymentId() + ")");
            
        System.out.println();
        System.out.println("===============================================================");
        System.out.println("📧 NEW EMAIL NOTIFICATION");
        System.out.println("===============================================================");
        System.out.println("To:      " + recipient);
        System.out.println("Subject: " + request.getTitle());
        System.out.println("Type:    " + request.getType());
        System.out.println("---------------------------------------------------------------");
        System.out.println(request.getMessage());
        System.out.println("===============================================================");
        System.out.println();
    }
}
