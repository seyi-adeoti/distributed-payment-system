package com.example.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all business-level errors.
 * Carries an HTTP status so the global handler can map it directly.
 * Services can extend this for domain-specific errors.
 */
public class ServiceException extends RuntimeException {

    private final HttpStatus status;

    public ServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ServiceException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
