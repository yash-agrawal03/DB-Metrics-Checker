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
        if (metricsByDatabase == null || metricsByDatabase.isEmpty()) {
            return "No benchmark data available";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Benchmark Results: %s (%d records) ===\n\n",
            operationType.getDisplayName(), actualRecordCount));
        
        metricsByDatabase.forEach((db, metrics) -> {
            sb.append(metrics.getSummary()).append("\n");
        });
        
        if (fastestDatabase != null && slowestDatabase != null) {
            sb.append(String.format("\n🏆 Fastest: %s", fastestDatabase.getDisplayName()));
            if (relativePerformance != null) {
                relativePerformance.forEach((db, perf) -> {
                    if (db != fastestDatabase) {
                        sb.append(String.format("\n   %s: %.1fx slower", db.getDisplayName(), perf));
                    }
                });
            }
        }
        
        return sb.toString();
    }
}

