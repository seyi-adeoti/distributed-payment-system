package com.example.ledger.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_events")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class ProcessedEvent {

    @Id 
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String reference;

    @Column(nullable = false)
    private String eventType;

    @CreationTimestamp
    private LocalDateTime processedAt;
}
