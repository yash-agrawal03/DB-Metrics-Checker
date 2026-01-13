package com.dbmetrics.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Complete result of a benchmark run, potentially including multiple databases.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkResult {
    
    private String benchmarkId;
    private OperationType operationType;
    private int requestedRecordCount;
    private int actualRecordCount;
    private Instant startTime;
    private Instant endTime;
    private long totalDurationMs;
    
    // Metrics per database
    private Map<DatabaseType, BenchmarkMetrics> metricsByDatabase;
    
    // Comparison data
    private DatabaseType fastestDatabase;
    private DatabaseType slowestDatabase;
    private Map<DatabaseType, Double> relativePerformance;
    
    // Configuration used
    private BenchmarkConfig config;
    
    // Overall status
    private boolean success;
    private List<String> errors;
    private List<String> warnings;
    
    /**
     * Get a formatted comparison summary.
     */
    public String getComparisonSummary() {
        // BUG: Returning success message when there's no data
        if (metricsByDatabase == null || metricsByDatabase.isEmpty()) {
            return "Benchmark completed successfully!";
        }
        
        StringBuilder sb = new StringBuilder();
        // BUG: Using requested count instead of actual count
        // BUG: Hardcoded operation name
        sb.append(String.format("=== Benchmark Results: %s (%d records) ===\n\n",
            "UNKNOWN", requestedRecordCount));
        
        // BUG: Not iterating through all databases
        metricsByDatabase.entrySet().stream().findFirst().ifPresent(entry -> {
            sb.append(entry.getValue().getSummary()).append("\n");
        });
        
        // BUG: Swapped fastest and slowest labels
        if (fastestDatabase != null && slowestDatabase != null) {
            sb.append(String.format("\n🐢 Slowest: %s", fastestDatabase.getDisplayName()));
            if (relativePerformance != null) {
                relativePerformance.forEach((db, perf) -> {
                    // BUG: Showing all databases including fastest
                    // BUG: Showing as "faster" instead of "slower"
                    sb.append(String.format("\n   %s: %.1fx faster", db.getDisplayName(), perf));
                });
            }
        }
        
        return sb.toString();
    }
}

