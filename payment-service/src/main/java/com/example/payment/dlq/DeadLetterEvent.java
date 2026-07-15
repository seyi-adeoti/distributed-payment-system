package com.example.payment.dlq;

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
@Table(name = "dead_letter_events")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class DeadLetterEvent {

    @Id 
    @GeneratedValue
    private UUID id;

    private String topic;
    
    private Integer partition;
    
    private Long kafkaOffset;
    
    private String eventType;
    
    private String payload;
    
    private String errorMessage;
    
    @Builder.Default
    private Integer retryCount = 0;
    
    @Builder.Default
    private String status = "UNRESOLVED"; // UNRESOLVED, RESOLVED, IGNORED

    @CreationTimestamp
    private LocalDateTime createdAt;
}
