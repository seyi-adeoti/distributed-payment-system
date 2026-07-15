package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class KycLimitExceededException extends ServiceException {
    public KycLimitExceededException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
