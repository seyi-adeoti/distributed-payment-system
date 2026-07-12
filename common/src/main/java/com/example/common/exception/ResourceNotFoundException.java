package com.example.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist.
 * Maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends ServiceException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(
            String.format("%s not found with %s: '%s'", resource, field, value),
            HttpStatus.NOT_FOUND
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
