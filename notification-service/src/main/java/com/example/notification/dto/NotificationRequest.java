package com.example.notification.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private UUID walletId;
    private UUID paymentId;
    private NotificationType type;
    private String title;
    private String message;
}
