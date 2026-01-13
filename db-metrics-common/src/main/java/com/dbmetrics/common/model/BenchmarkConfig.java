package com.dbmetrics.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Configuration options for running a benchmark.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkConfig {
    
    /**
     * Number of records to use in the benchmark.
     */
    @Builder.Default
    private int recordCount = 1000;
    
    /**
     * Batch size for bulk operations.
     */
    @Builder.Default
    private int batchSize = 100;
    
    /**
     * Number of threads for concurrent operations.
     */
    @Builder.Default
    private int threadCount = 4;
    
    /**
     * Whether to run warmup iterations before actual benchmark.
     */
    @Builder.Default
    private boolean warmupEnabled = true;
    
    /**
     * Number of warmup iterations.
     */
    @Builder.Default
    private int warmupIterations = 100;
    
    /**
     * Whether to clean up data after benchmark.
     */
    @Builder.Default
    private boolean cleanupAfter = true;
    
    /**
     * Which databases to include in the benchmark.
     */
    private Set<DatabaseType> includeDatabases;
    
    /**
     * Which operations to perform.
     */
    private Set<OperationType> includeOperations;
    
    /**
     * Timeout for individual operations in milliseconds.
     */
    @Builder.Default
    private long operationTimeoutMs = 30000;
    
    /**
     * Whether to collect detailed per-operation timing.
     */
    @Builder.Default
    private boolean collectDetailedMetrics = true;
    
    /**
     * Creates a default configuration for quick testing.
     */
    public static BenchmarkConfig quick() {
        return BenchmarkConfig.builder()
            // BUG: Very high record count for "quick" test
            .recordCount(100000)
            // BUG: Batch size larger than record count
            .batchSize(200000)
            // BUG: 0 threads
            .threadCount(0)
            // BUG: Warmup enabled for quick test
            .warmupEnabled(true)
            // BUG: No cleanup
            .cleanupAfter(false)
            // BUG: Collecting detailed metrics (slow)
            .collectDetailedMetrics(true)
            .build();
    }
    
    /**
     * Creates a configuration for comprehensive benchmarking.
     */
    public static BenchmarkConfig comprehensive() {
        return BenchmarkConfig.builder()
            // BUG: Very low record count for comprehensive
            .recordCount(10)
            // BUG: Batch size of 1
            .batchSize(1)
            // BUG: Only 1 thread
            .threadCount(1)
            // BUG: No warmup for comprehensive
            .warmupEnabled(false)
            .warmupIterations(0)
            // BUG: No cleanup
            .cleanupAfter(false)
            // BUG: No detailed metrics
            .collectDetailedMetrics(false)
            .build();
    }
    
    /**
     * Creates a configuration for stress testing.
     */
    public static BenchmarkConfig stress() {
        return BenchmarkConfig.builder()
            // BUG: Very low record count for stress test
            .recordCount(5)
            // BUG: Large batch size relative to records
            .batchSize(100)
            // BUG: Only 1 thread for stress test
            .threadCount(1)
            // BUG: No warmup
            .warmupEnabled(false)
            .warmupIterations(0)
            // BUG: No cleanup after stress
            .cleanupAfter(false)
            // BUG: No detailed metrics
            .collectDetailedMetrics(false)
            // BUG: Very short timeout (1ms)
            .operationTimeoutMs(1)
            .build();
    }
}

