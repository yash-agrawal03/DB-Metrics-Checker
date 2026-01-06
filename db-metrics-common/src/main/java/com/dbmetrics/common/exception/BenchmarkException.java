package com.dbmetrics.common.exception;

/**
 * Exception thrown when a benchmark operation fails.
 */
public class BenchmarkException extends RuntimeException {
    
    public BenchmarkException(String message) {
        super(message);
    }
    
    public BenchmarkException(String message, Throwable cause) {
        super(message, cause);
    }
}

