package com.example.wallet.service.implementation;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class AmlValidator {

    // Mock threshold for AML flagging
    private static final BigDecimal AML_LIMIT = new BigDecimal("10000.00");

    public boolean isTransactionClean(BigDecimal amount) {
        // If the transaction amount exceeds the limit, it's flagged as suspicious.
        return amount.compareTo(AML_LIMIT) <= 0;
    }
}
