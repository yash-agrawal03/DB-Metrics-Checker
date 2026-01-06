package com.dbmetrics.api.config;

import com.dbmetrics.common.exception.BenchmarkException;
import com.dbmetrics.common.exception.DatabaseOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                (existing, replacement) -> existing
            ));
        
        return ResponseEntity.badRequest().body(Map.of(
            "status", "error",
            "message", "Validation failed",
            "errors", errors,
            "timestamp", Instant.now().toString()
        ));
    }
    
    @ExceptionHandler(DatabaseOperationException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseOperationException(DatabaseOperationException ex) {
        log.error("Database operation failed", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "status", "error",
            "message", "Database operation failed",
            "database", ex.getDatabaseType().getDisplayName(),
            "operation", ex.getOperation(),
            "timestamp", Instant.now().toString()
        ));
    }
    
    @ExceptionHandler(BenchmarkException.class)
    public ResponseEntity<Map<String, Object>> handleBenchmarkException(BenchmarkException ex) {
        log.error("Benchmark failed", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "status", "error",
            "message", "Benchmark failed",
            "details", ex.getMessage(),
            "timestamp", Instant.now().toString()
        ));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "status", "error",
            "message", ex.getMessage(),
            "timestamp", Instant.now().toString()
        ));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "status", "error",
            "message", "An unexpected error occurred",
            "timestamp", Instant.now().toString()
        ));
    }
}

