package com.dbmetrics.common.model;

/**
 * Enumeration of supported database types for benchmarking.
 */
public enum DatabaseType {
    MYSQL("MySQL", "Relational Database"),
    REDIS("Redis", "In-Memory Key-Value Store"),
    AEROSPIKE("Aerospike", "High-Performance NoSQL Database");
    
    private final String displayName;
    private final String description;
    
    DatabaseType(String displayName, String description) {
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

