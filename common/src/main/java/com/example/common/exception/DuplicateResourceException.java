package com.example.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation would create a duplicate resource.
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends ServiceException {

    public DuplicateResourceException(String resource, String field, Object value) {
        super(
            String.format("%s already exists with %s: '%s'", resource, field, value),
            HttpStatus.CONFLICT
        );
    }

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
