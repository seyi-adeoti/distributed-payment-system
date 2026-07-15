package com.example.common.event;

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
public class PaymentReversedEvent {
    private UUID paymentId;
    private String reference;
    private String reason;
    private LocalDateTime occurredAt;
}
