package com.example.wallet.service.implementation;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AmlValidator {

    // Mock threshold for AML flagging
    private static final BigDecimal AML_LIMIT = new BigDecimal("10000.00");

    @CircuitBreaker(name = "amlService", fallbackMethod = "fallbackAml")
    public boolean isTransactionClean(BigDecimal amount) {
        // Mock external call that could potentially fail or timeout
        // If the transaction amount exceeds the limit, it's flagged as suspicious.
        return amount.compareTo(AML_LIMIT) <= 0;
    }

    // Fallback method must have the same signature + Throwable parameter
    public boolean fallbackAml(BigDecimal amount, Throwable t) {
        log.error("AML Service circuit breaker open or failed! Fallback triggered for amount {}. Error: {}", amount, t.getMessage());
        // In a real system, you might reject the transaction, queue it for manual review, 
        // or accept it up to a smaller safe limit. For our mock, we'll reject to be safe.
        return false;
    }
}
