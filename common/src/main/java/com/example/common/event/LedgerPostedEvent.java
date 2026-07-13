package com.example.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerPostedEvent {
    private UUID paymentId;
    private String reference;
    private String debitAccountCode;
    private String creditAccountCode;
    private BigDecimal amount;
    private LocalDateTime occurredAt;
}
