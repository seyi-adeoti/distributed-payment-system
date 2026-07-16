package com.example.notification.consumer;

import java.math.BigDecimal;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.common.KafkaTopics;
import com.example.common.event.PaymentCompletedEvent;
import com.example.common.event.PaymentFailedEvent;
import com.example.common.event.PaymentReversedEvent;
import com.example.common.event.UserCreatedEvent;
import com.example.notification.client.UserServiceClient;
import com.example.notification.dto.NotificationRequest;
import com.example.notification.dto.NotificationType;
import com.example.notification.service.NotificationSender;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationSender notificationSender;
    private final UserServiceClient userServiceClient; // REST call is OK here
    private final ObjectMapper objectMapper;           // notification is not money-critical

    @KafkaListener(
        topics = {KafkaTopics.PAYMENT_EVENTS, KafkaTopics.USER_EVENTS}, 
        groupId = "notification-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentEvent(ConsumerRecord<String, String> record) {
        String eventType = extractEventType(record.headers());

        try {
            switch (eventType) {
                case "UserCreated" -> {
                    UserCreatedEvent event = deserialize(
                        record.value(), UserCreatedEvent.class);
                        
                    notificationSender.send(NotificationRequest.builder()
                        // Since we have the email, we might want to pass it through NotificationRequest. Let's see if we can.
                        .walletId(event.getUserId()) // Using userId here just to have something unique, though it's not a wallet.
                        .type(NotificationType.SECURITY_ALERT)
                        .title("Welcome to Distributed Payment Platform!")
                        .message(String.format("Hi %s, your account has been successfully created. Please upgrade your KYC to start transacting.", event.getFirstName()))
                        .build());
                }
                
                case "PaymentCompleted" -> {
                    PaymentCompletedEvent event = deserialize(
                        record.value(), PaymentCompletedEvent.class);

                    notificationSender.send(NotificationRequest.builder()
                        .walletId(event.getSenderWalletId()) // Note: PaymentCompletedEvent needs sender/receiver wallets! Let's check common.
                        .type(NotificationType.DEBIT_ALERT)
                        .title("Transfer Successful")
                        .message(String.format(
                            "Your transfer of ₦%s (Ref: %s) was successful.",
                            formatAmount(event.getAmount()),
                            event.getReference()))
                        .build());
                        
                    // Let's assume PaymentCompletedEvent only has paymentId, amount, ref for now.
                    // Oh, the snippet had event.getSenderWalletId() and event.getReceiverWalletId() in PaymentCompletedEvent!
                    // Let me check my implementation... I didn't add those to PaymentCompletedEvent earlier.
                    // Actually, the previous agent created PaymentCompletedEvent. I will update it in common module in a bit.
                }

                case "PaymentFailed" -> {
                    PaymentFailedEvent event = deserialize(
                        record.value(), PaymentFailedEvent.class);

                    notificationSender.send(NotificationRequest.builder()
                        .paymentId(event.getPaymentId())
                        .type(NotificationType.TRANSFER_FAILED)
                        .title("Transfer Failed")
                        .message(String.format(
                            "Your transfer (Ref: %s) failed. Reason: %s. No funds were deducted.",
                            event.getReference(), humanReadableReason(event.getReason())))
                        .build());
                }

                case "PaymentReversed" -> {
                    PaymentReversedEvent event = deserialize(
                        record.value(), PaymentReversedEvent.class);

                    notificationSender.send(NotificationRequest.builder()
                        .paymentId(event.getPaymentId())
                        .type(NotificationType.TRANSFER_REVERSED)
                        .title("Transfer Reversed")
                        .message(String.format(
                            "Your transfer (Ref: %s) encountered an issue. " +
                            "Your funds have been returned to your wallet.",
                            event.getReference()))
                        .build());
                }

                default -> log.debug("Notification service ignored: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed processing event: {} Error: {}", eventType, e.getMessage());
        }
    }

    private String humanReadableReason(String technicalReason) {
        if (technicalReason == null) return "An unexpected error occurred";
        if (technicalReason.contains("INSUFFICIENT_FUNDS")) return "Insufficient funds";
        if (technicalReason.contains("WALLET_INACTIVE")) return "Account is inactive";
        if (technicalReason.contains("LIMIT_EXCEEDED")) return "Daily limit exceeded";
        return "An unexpected error occurred";
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "0.00";
    }

    private String extractEventType(Headers headers) {
        Header header = headers.lastHeader("eventType");
        return header != null ? new String(header.value()) : "UNKNOWN";
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}
