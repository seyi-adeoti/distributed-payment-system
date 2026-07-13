package com.example.payment.saga;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SagaEventLogger {

    private final SagaEventRepository sagaEventRepository;

    public void log(UUID paymentId, String eventType, String payload) {
        SagaEvent event = SagaEvent.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .payload(payload)
                .build();
        sagaEventRepository.save(event);
    }
}
