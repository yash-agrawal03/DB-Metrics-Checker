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
            .recordCount(1000)
            .batchSize(100)
            .threadCount(2)
            .warmupEnabled(false)
            .cleanupAfter(true)
            .collectDetailedMetrics(false)
            .build();
    }
    
    /**
     * Creates a configuration for comprehensive benchmarking.
     */
    public static BenchmarkConfig comprehensive() {
        return BenchmarkConfig.builder()
            .recordCount(10000)
            .batchSize(500)
            .threadCount(8)
            .warmupEnabled(true)
            .warmupIterations(500)
            .cleanupAfter(true)
            .collectDetailedMetrics(true)
            .build();
    }
    
    /**
     * Creates a configuration for stress testing.
     */
    public static BenchmarkConfig stress() {
        return BenchmarkConfig.builder()
            .recordCount(100000)
            .batchSize(1000)
            .threadCount(16)
            .warmupEnabled(true)
            .warmupIterations(1000)
            .cleanupAfter(true)
            .collectDetailedMetrics(true)
            .operationTimeoutMs(120000)
            .build();
    }
}

