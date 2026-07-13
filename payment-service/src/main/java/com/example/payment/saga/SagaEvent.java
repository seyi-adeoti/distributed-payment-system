package com.example.payment.saga;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saga_events")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class SagaEvent {

    @Id 
    @GeneratedValue
    private UUID id;

    private UUID paymentId;
    
    private String eventType;
    
    private String payload;

    @CreationTimestamp
    private LocalDateTime processedAt;
}
