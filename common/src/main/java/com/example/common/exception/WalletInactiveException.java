package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class WalletInactiveException extends ServiceException {
    public WalletInactiveException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
