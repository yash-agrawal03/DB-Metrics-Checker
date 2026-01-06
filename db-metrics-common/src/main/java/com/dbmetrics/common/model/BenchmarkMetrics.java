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
        if (recordCount == 0) return 0.0;
        return (double) successCount / recordCount * 100.0;
    }
    
    /**
     * Get a human-readable summary of the benchmark.
     */
    public String getSummary() {
        return String.format(
            "%s %s: %d records in %dms (%.2f ops/sec, avg: %.2fms, p99: %.2fms)",
            databaseType.getDisplayName(),
            operationType.getDisplayName(),
            recordCount,
            totalTimeMs,
            operationsPerSecond,
            averageTimeMs,
            p99TimeMs
        );
    }
}

