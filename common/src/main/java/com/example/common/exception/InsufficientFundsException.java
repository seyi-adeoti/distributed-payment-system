package com.example.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a wallet has insufficient funds for a debit operation.
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class InsufficientFundsException extends ServiceException {

    public InsufficientFundsException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public InsufficientFundsException(String walletId, String amount) {
        super(
            String.format("Insufficient funds in wallet %s for amount %s", walletId, amount),
            HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}
