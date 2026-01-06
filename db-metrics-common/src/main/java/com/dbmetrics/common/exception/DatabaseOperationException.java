package com.dbmetrics.common.exception;

import com.dbmetrics.common.model.DatabaseType;

/**
 * Exception thrown when a database operation fails.
 */
public class DatabaseOperationException extends RuntimeException {
    
    private final DatabaseType databaseType;
    private final String operation;
    
    public DatabaseOperationException(DatabaseType databaseType, String operation, String message) {
        super(String.format("[%s] %s failed: %s", databaseType.getDisplayName(), operation, message));
        this.databaseType = databaseType;
        this.operation = operation;
    }
    
    public DatabaseOperationException(DatabaseType databaseType, String operation, String message, Throwable cause) {
        super(String.format("[%s] %s failed: %s", databaseType.getDisplayName(), operation, message), cause);
        this.databaseType = databaseType;
        this.operation = operation;
    }
    
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
    
    public String getOperation() {
        return operation;
    }
}

