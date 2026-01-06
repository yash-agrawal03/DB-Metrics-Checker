package com.dbmetrics.common.operations;

import com.dbmetrics.common.model.BenchmarkRecord;
import com.dbmetrics.common.model.DatabaseType;

import java.util.List;
import java.util.Optional;

/**
 * Core interface that all database modules must implement.
 * Provides a unified API for database operations across different database types.
 */
public interface DatabaseOperations {
    
    /**
     * Get the type of database this implementation handles.
     */
    DatabaseType getDatabaseType();
    
    /**
     * Check if the database connection is healthy.
     */
    boolean isHealthy();
    
    /**
     * Initialize the database schema/structure if needed.
     */
    void initialize();
    
    /**
     * Clean up all benchmark data.
     */
    void cleanup();
    
    // ==================== Single Record Operations ====================
    
    /**
     * Write a single record to the database.
     * @param record The record to write
     * @return The ID of the written record
     */
    String write(BenchmarkRecord record);
    
    /**
     * Read a single record by ID.
     * @param id The record ID
     * @return The record if found
     */
    Optional<BenchmarkRecord> read(String id);
    
    /**
     * Update an existing record.
     * @param record The record with updated values
     * @return true if update was successful
     */
    boolean update(BenchmarkRecord record);
    
    /**
     * Delete a record by ID.
     * @param id The record ID
     * @return true if deletion was successful
     */
    boolean delete(String id);
    
    // ==================== Bulk Operations ====================
    
    /**
     * Write multiple records in batch.
     * @param records The records to write
     * @return List of IDs of successfully written records
     */
    List<String> writeBatch(List<BenchmarkRecord> records);
    
    /**
     * Read multiple records by IDs.
     * @param ids The record IDs
     * @return List of found records
     */
    List<BenchmarkRecord> readBatch(List<String> ids);
    
    /**
     * Read all records (use with caution for large datasets).
     * @param limit Maximum number of records to return
     * @return List of records
     */
    List<BenchmarkRecord> readAll(int limit);
    
    /**
     * Update multiple records in batch.
     * @param records The records to update
     * @return Number of successfully updated records
     */
    int updateBatch(List<BenchmarkRecord> records);
    
    /**
     * Delete multiple records by IDs.
     * @param ids The record IDs
     * @return Number of successfully deleted records
     */
    int deleteBatch(List<String> ids);
    
    // ==================== Utility Operations ====================
    
    /**
     * Count total records in the database.
     */
    long count();
    
    /**
     * Check if a record exists.
     * @param id The record ID
     */
    boolean exists(String id);
    
}

