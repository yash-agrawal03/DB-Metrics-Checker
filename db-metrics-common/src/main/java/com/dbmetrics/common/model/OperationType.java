package com.dbmetrics.common.model;

/**
 * Types of database operations that can be benchmarked.
 */
public enum OperationType {
    WRITE("Write", "Insert new records"),
    READ("Read", "Retrieve existing records"),
    UPDATE("Update", "Modify existing records"),
    DELETE("Delete", "Remove records"),
    BULK_WRITE("Bulk Write", "Insert multiple records in batch"),
    BULK_READ("Bulk Read", "Retrieve multiple records in batch"),
    CONCURRENT_READ_WRITE("Concurrent R/W", "Simultaneous read and write operations");
    
    private final String displayName;
    private final String description;
    
    OperationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}

