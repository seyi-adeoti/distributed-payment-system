package com.example.payment.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.example.payment.enum.OutboxStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;



@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id @GeneratedValue
    private UUID id;

    private UUID aggregateId;       // e.g. paymentId
    private String aggregateType;   // e.g. "Payment"
    private String eventType;       // e.g. "PaymentInitiated"

    @Column(columnDefinition = "TEXT")
    private String payload;         // JSON of the event

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;    // PENDING, PUBLISHED, FAILED

    private int retryCount;
    private String lastError;

    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}

