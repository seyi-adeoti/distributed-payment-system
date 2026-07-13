package com.example.payment.saga;

public enum SagaStep {
    INITIATED,
    DEBIT_PENDING,
    DEBIT_COMPLETED,
    LEDGER_PENDING,
    COMPLETED,
    COMPENSATION_PENDING,
    REVERSED,
    FAILED
}
