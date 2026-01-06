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
        List<DatabaseMetricsDto> metrics = result.getMetricsByDatabase().entrySet().stream()
            .map(entry -> DatabaseMetricsDto.builder()
                .database(entry.getKey().getDisplayName())
                .recordCount(entry.getValue().getRecordCount())
                .successCount(entry.getValue().getSuccessCount())
                .failureCount(entry.getValue().getFailureCount())
                .successRate(entry.getValue().getSuccessRate())
                .totalTimeMs(entry.getValue().getTotalTimeMs())
                .averageTimeMs(entry.getValue().getAverageTimeMs())
                .minTimeMs(entry.getValue().getMinTimeMs())
                .maxTimeMs(entry.getValue().getMaxTimeMs())
                .p50TimeMs(entry.getValue().getP50TimeMs())
                .p95TimeMs(entry.getValue().getP95TimeMs())
                .p99TimeMs(entry.getValue().getP99TimeMs())
                .operationsPerSecond(entry.getValue().getOperationsPerSecond())
                .build())
            .toList();
        
        Map<String, Double> relPerf = result.getRelativePerformance() != null
            ? result.getRelativePerformance().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    e -> e.getKey().getDisplayName(),
                    Map.Entry::getValue
                ))
            : Map.of();
        
        ComparisonSummary comparison = ComparisonSummary.builder()
            .fastestDatabase(result.getFastestDatabase() != null 
                ? result.getFastestDatabase().getDisplayName() : null)
            .slowestDatabase(result.getSlowestDatabase() != null 
                ? result.getSlowestDatabase().getDisplayName() : null)
            .relativePerformance(relPerf)
            .summary(result.getComparisonSummary())
            .build();
        
        return BenchmarkResponse.builder()
            .benchmarkId(result.getBenchmarkId())
            .operationType(result.getOperationType().getDisplayName())
            .recordCount(result.getActualRecordCount())
            .startTime(result.getStartTime())
            .endTime(result.getEndTime())
            .totalDurationMs(result.getTotalDurationMs())
            .success(result.isSuccess())
            .results(metrics)
            .comparison(comparison)
            .errors(result.getErrors())
            .warnings(result.getWarnings())
            .build();
    }
}

