package com.example.payment.saga;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

@Entity
@Table(name = "payment_saga")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class PaymentSaga {

    @Id 
    @GeneratedValue
    private UUID id;

    @Column(unique = true)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    private SagaStep currentStep;

    private LocalDateTime debitCompletedAt;
    private LocalDateTime ledgerPostedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private String failureReason;
    private String compensationReason;
    private LocalDateTime reversedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
