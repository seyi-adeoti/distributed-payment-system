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
public class DebitReversalRequestedEvent {
    private UUID paymentId;
    private UUID senderWalletId;
    private BigDecimal amount;
    private String reference;
    private String reason;
    private LocalDateTime occurredAt;
}
