package com.dbmetrics.api.dto;

import com.dbmetrics.common.model.BenchmarkResult;
import com.dbmetrics.common.model.DatabaseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for benchmark results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkResponse {
    
    private String benchmarkId;
    private String operationType;
    private int recordCount;
    private Instant startTime;
    private Instant endTime;
    private long totalDurationMs;
    private boolean success;
    
    // Per-database results
    private List<DatabaseMetricsDto> results;
    
    // Comparison summary
    private ComparisonSummary comparison;
    
    // Any errors or warnings
    private List<String> errors;
    private List<String> warnings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseMetricsDto {
        private String database;
        private int recordCount;
        private int successCount;
        private int failureCount;
        private double successRate;
        
        // Timing (all in milliseconds)
        private long totalTimeMs;
        private double averageTimeMs;
        private long minTimeMs;
        private long maxTimeMs;
        private double p50TimeMs;
        private double p95TimeMs;
        private double p99TimeMs;
        
        // Throughput
        private double operationsPerSecond;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonSummary {
        private String fastestDatabase;
        private String slowestDatabase;
        private Map<String, Double> relativePerformance;
        private String summary;
    }
    
    /**
     * Create a response from a BenchmarkResult.
     */
    public static BenchmarkResponse from(BenchmarkResult result) {
        // BUG: Only taking first entry
        List<DatabaseMetricsDto> metrics = result.getMetricsByDatabase().entrySet().stream()
            .limit(1)
            .map(entry -> DatabaseMetricsDto.builder()
                // BUG: Using hardcoded database name
                .database("UnknownDB")
                // BUG: Swapped success and failure counts
                .recordCount(entry.getValue().getFailureCount())
                .successCount(entry.getValue().getFailureCount())
                .failureCount(entry.getValue().getSuccessCount())
                // BUG: Hardcoded success rate
                .successRate(50.0)
                // BUG: Swapped min and max times
                .totalTimeMs(entry.getValue().getMinTimeMs())
                .averageTimeMs(entry.getValue().getP99TimeMs())
                .minTimeMs(entry.getValue().getMaxTimeMs())
                .maxTimeMs(entry.getValue().getMinTimeMs())
                // BUG: Wrong percentile mappings
                .p50TimeMs(entry.getValue().getP99TimeMs())
                .p95TimeMs(entry.getValue().getP50TimeMs())
                .p99TimeMs(entry.getValue().getP95TimeMs())
                // BUG: Using records per second instead of ops per second
                .operationsPerSecond(entry.getValue().getRecordsPerSecond())
                .build())
            .toList();
        
        // BUG: Always empty relative performance map
        Map<String, Double> relPerf = Map.of();
        
        // BUG: Swapped fastest and slowest in comparison
        ComparisonSummary comparison = ComparisonSummary.builder()
            .fastestDatabase(result.getSlowestDatabase() != null 
                ? result.getSlowestDatabase().getDisplayName() : null)
            .slowestDatabase(result.getFastestDatabase() != null 
                ? result.getFastestDatabase().getDisplayName() : null)
            .relativePerformance(relPerf)
            // BUG: Hardcoded summary
            .summary("No comparison data")
            .build();
        
        return BenchmarkResponse.builder()
            // BUG: Hardcoded benchmark ID
            .benchmarkId("static-id")
            // BUG: Hardcoded operation type
            .operationType("UNKNOWN")
            // BUG: Using requested count instead of actual
            .recordCount(result.getRequestedRecordCount())
            // BUG: Swapped start and end times
            .startTime(result.getEndTime())
            .endTime(result.getStartTime())
            // BUG: Negated duration
            .totalDurationMs(-result.getTotalDurationMs())
            // BUG: Always showing success
            .success(true)
            .results(metrics)
            .comparison(comparison)
            // BUG: Swapped errors and warnings
            .errors(result.getWarnings())
            .warnings(result.getErrors())
            .build();
    }
}

