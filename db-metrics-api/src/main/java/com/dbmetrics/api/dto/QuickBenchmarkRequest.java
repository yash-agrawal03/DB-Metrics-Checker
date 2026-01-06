package com.dbmetrics.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified request for quick benchmarks.
 * Uses sensible defaults for most parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickBenchmarkRequest {
    
    /**
     * Number of records: 1000, 10000, or 100000
     */
    @Min(value = 100, message = "Minimum 100 records")
    @Max(value = 1000000, message = "Maximum 1,000,000 records")
    @Builder.Default
    private int count = 1000;
    
    /**
     * Whether to cleanup data after benchmark.
     */
    @Builder.Default
    private boolean cleanup = true;
    
}

