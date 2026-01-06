package com.dbmetrics.api.dto;

import com.dbmetrics.common.model.DatabaseType;
import com.dbmetrics.common.model.OperationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Request DTO for running benchmarks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkRequest {
    
    @NotNull(message = "Operation type is required")
    private OperationType operationType;
    
    @Min(value = 1, message = "Record count must be at least 1")
    @Max(value = 1000000, message = "Record count cannot exceed 1,000,000")
    @Builder.Default
    private int recordCount = 1000;
    
    @Min(value = 1, message = "Batch size must be at least 1")
    @Max(value = 10000, message = "Batch size cannot exceed 10,000")
    @Builder.Default
    private int batchSize = 100;
    
    @Min(value = 1, message = "Thread count must be at least 1")
    @Max(value = 64, message = "Thread count cannot exceed 64")
    @Builder.Default
    private int threadCount = 4;
    
    @Builder.Default
    private boolean warmupEnabled = true;
    
    @Min(value = 0)
    @Max(value = 1000)
    @Builder.Default
    private int warmupIterations = 100;
    
    @Builder.Default
    private boolean cleanupAfter = true;
    
    /**
     * Databases to include. If empty, all available databases are used.
     */
    private Set<DatabaseType> databases;
    
}

