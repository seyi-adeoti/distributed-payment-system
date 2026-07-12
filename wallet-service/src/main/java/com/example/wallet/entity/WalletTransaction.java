package com.example.wallet.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID transactionId;

    @Column(nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;  // DEBIT, CREDIT, REVERSAL

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String reference;

    private String narration;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum TransactionType {
        DEBIT, CREDIT, REVERSAL
    }
}
