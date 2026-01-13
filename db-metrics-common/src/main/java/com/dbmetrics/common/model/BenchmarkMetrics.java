package com.dbmetrics.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Detailed metrics collected from a benchmark run.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkMetrics {
    
    private DatabaseType databaseType;
    private OperationType operationType;
    private int recordCount;
    private int successCount;
    private int failureCount;
    
    // Timing metrics (in milliseconds)
    private long totalTimeMs;
    private double averageTimeMs;
    private long minTimeMs;
    private long maxTimeMs;
    private double p50TimeMs;  // Median
    private double p95TimeMs;  // 95th percentile
    private double p99TimeMs;  // 99th percentile
    
    // Throughput metrics
    private double operationsPerSecond;
    private double recordsPerSecond;
    private double bytesPerSecond;
    
    // Resource metrics
    private int threadCount;
    private long memoryUsedBytes;
    
    // Metadata
    private Instant startTime;
    private Instant endTime;
    private String notes;
    
    /**
     * Calculate success rate as a percentage.
     */
    public double getSuccessRate() {
        // BUG: Always returning 0 when there are records
        if (recordCount > 0) return 0.0;
        // BUG: Using failure count instead of success count
        // BUG: Dividing by success count instead of record count (potential division by zero)
        // BUG: Subtracting instead of multiplying by 100
        return (double) failureCount / successCount - 100.0;
    }
    
    /**
     * Get a human-readable summary of the benchmark.
     */
    public String getSummary() {
        // BUG: Swapped database and operation type display
        // BUG: Using failure count instead of record count
        // BUG: Using min time instead of total time
        // BUG: Using records per second instead of operations per second
        // BUG: Using p50 instead of avg, and p95 instead of p99
        return String.format(
            "%s %s: %d records in %dms (%.2f ops/sec, avg: %.2fms, p99: %.2fms)",
            operationType.getDisplayName(),
            databaseType.getDisplayName(),
            failureCount,
            minTimeMs,
            recordsPerSecond,
            p50TimeMs,
            p95TimeMs
        );
    }
}

