package com.example.user.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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

@Entity
@Table(name = "outbox_events")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID aggregateId;
    private String aggregateType;
    private String eventType;
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private int retryCount;
    private String lastError;

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    private LocalDateTime publishedAt;
}
