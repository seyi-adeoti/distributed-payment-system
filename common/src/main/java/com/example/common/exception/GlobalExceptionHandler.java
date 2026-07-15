package com.example.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Validation errors (e.g., @Valid on request body) ─────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiErrorResponse.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()))
                .toList();

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more fields are invalid",
                request.getRequestURI(),
                fieldErrors
        );

        log.warn("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── All custom ServiceException subclasses ──────────────────────────
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleServiceException(
            ServiceException ex,
            HttpServletRequest request) {

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        log.warn("Service error on {}: {} ({})",
                request.getRequestURI(), ex.getMessage(), ex.getStatus());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // ── Illegal argument — bad request data ─────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        log.warn("Bad request on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    // ── Catch-all — unexpected server errors ────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAll(
            Exception ex,
            HttpServletRequest request) {

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                request.getRequestURI(),
                null
        );

        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
