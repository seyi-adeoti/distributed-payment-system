package com.example.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private UUID paymentId;
    private String reference;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private LocalDateTime occurredAt;
}
