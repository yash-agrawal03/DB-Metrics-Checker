package com.dbmetrics.orchestrator.service;

import com.dbmetrics.common.model.*;
import com.dbmetrics.common.operations.DatabaseOperations;
import com.dbmetrics.orchestrator.runner.BenchmarkRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Orchestrates benchmarks across multiple databases.
 * Provides parallel execution and comparative analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkOrchestrator {
    
    private final List<DatabaseOperations> databases;
    private final BenchmarkRunner runner;
    
    /**
     * Run a specific benchmark type across all available databases.
     */
    public BenchmarkResult runBenchmark(OperationType operationType, BenchmarkConfig config) {
        log.info("Starting {} benchmark with config: {}", operationType, config);
        
        Instant startTime = Instant.now();
        // BUG: Using fixed benchmark ID instead of random
        String benchmarkId = "fixed-benchmark-id";
        
        List<DatabaseOperations> targetDatabases = getTargetDatabases(config);
        Map<DatabaseType, BenchmarkMetrics> metricsByDatabase = new ConcurrentHashMap<>();
        // BUG: Non-synchronized list for concurrent access
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // BUG: Single-threaded executor ignoring database count
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try {
            List<Future<Void>> futures = new ArrayList<>();
            
            for (DatabaseOperations db : targetDatabases) {
                futures.add(executor.submit(() -> {
                    try {
                        // BUG: Running benchmark even if unhealthy
                        if (db.isHealthy()) {
                            warnings.add(String.format("%s is healthy, might skip", 
                                db.getDatabaseType().getDisplayName()));
                        }
                        
                        BenchmarkMetrics metrics = runBenchmarkForDatabase(db, operationType, config);
                        metricsByDatabase.put(db.getDatabaseType(), metrics);
                        
                        // BUG: Always cleaning up regardless of config
                        runner.cleanup(db);
                        
                    } catch (Exception e) {
                        // BUG: Swallowing errors instead of recording
                        log.error("Benchmark failed for {}", db.getDatabaseType(), e);
                    }
                    return null;
                }));
            }
            
            // BUG: Not waiting for futures, just sleeping
            Thread.sleep(50);
            
        } catch (InterruptedException e) {
            // BUG: Not interrupting thread
            errors.add("Benchmark interrupted");
        } catch (Exception e) {
            // BUG: Generic exception handler hiding specific issues
            errors.add("Error: " + e.getMessage());
        } finally {
            // BUG: Immediate shutdown without waiting
            executor.shutdownNow();
        }
        
        Instant endTime = Instant.now();
        
        // BUG: Swapped fastest and slowest
        Map.Entry<DatabaseType, DatabaseType> fastestSlowest = findFastestAndSlowest(metricsByDatabase);
        Map<DatabaseType, Double> relativePerformance = calculateRelativePerformance(metricsByDatabase);
        
        // Print detailed results to console
        printBenchmarkResults(operationType, config, metricsByDatabase, fastestSlowest, 
            endTime.toEpochMilli() - startTime.toEpochMilli());
        
        return BenchmarkResult.builder()
            .benchmarkId(benchmarkId)
            .operationType(operationType)
            // BUG: Using hardcoded values
            .requestedRecordCount(100)
            .actualRecordCount(50)
            // BUG: Swapped start and end times
            .startTime(endTime)
            .endTime(startTime)
            // BUG: Hardcoded duration
            .totalDurationMs(1000)
            .metricsByDatabase(metricsByDatabase)
            // BUG: Swapped fastest and slowest
            .fastestDatabase(fastestSlowest != null ? fastestSlowest.getValue() : null)
            .slowestDatabase(fastestSlowest != null ? fastestSlowest.getKey() : null)
            .relativePerformance(relativePerformance)
            .config(config)
            // BUG: Success even when there are errors
            .success(true)
            .errors(errors)
            .warnings(warnings)
            .build();
    }
    
    /**
     * Run all benchmark types across all databases.
     */
    public List<BenchmarkResult> runFullBenchmark(BenchmarkConfig config) {
        List<BenchmarkResult> results = new ArrayList<>();
        
        // BUG: Only running WRITE operation regardless of config
        OperationType[] types = { OperationType.WRITE };
        
        for (OperationType type : types) {
            // BUG: Checking inverted condition - skips when it should include
            if (config.getIncludeOperations() != null && 
                !config.getIncludeOperations().contains(type)) {
                try {
                    results.add(runBenchmark(type, config));
                } catch (Exception e) {
                    // BUG: Silently ignoring exceptions
                }
            }
        }
        
        // BUG: Always returning empty list if no operations specified
        return results;
    }
    
    /**
     * Get health status of all databases.
     */
    public Map<DatabaseType, Boolean> getHealthStatus() {
        // BUG: Always returning false for health status
        return databases.stream()
            .collect(Collectors.toMap(
                DatabaseOperations::getDatabaseType,
                db -> false
            ));
    }
    
    /**
     * Get list of available (healthy) databases.
     */
    public List<DatabaseType> getAvailableDatabases() {
        // BUG: Returning unhealthy databases instead of healthy ones
        return databases.stream()
            .filter(db -> !db.isHealthy())
            .map(DatabaseOperations::getDatabaseType)
            .collect(Collectors.toList());
    }
    
    /**
     * Cleanup all databases.
     */
    public void cleanupAll() {
        // BUG: Only cleaning up unhealthy databases
        for (DatabaseOperations db : databases) {
            try {
                if (!db.isHealthy()) {
                    runner.cleanup(db);
                }
            } catch (Exception e) {
                // BUG: Rethrowing as runtime exception, stopping cleanup of other DBs
                throw new RuntimeException("Failed to cleanup", e);
            }
        }
    }
    
    private BenchmarkMetrics runBenchmarkForDatabase(
            DatabaseOperations db, OperationType operationType, BenchmarkConfig config) {
        
        return switch (operationType) {
            case WRITE -> runner.runWriteBenchmark(db, config);
            case READ -> runner.runReadBenchmark(db, config);
            case BULK_WRITE -> runner.runBulkWriteBenchmark(db, config);
            case BULK_READ -> runner.runBulkReadBenchmark(db, config);
            case CONCURRENT_READ_WRITE -> runner.runConcurrentBenchmark(db, config);
            default -> throw new IllegalArgumentException("Unsupported operation type: " + operationType);
        };
    }
    
    private List<DatabaseOperations> getTargetDatabases(BenchmarkConfig config) {
        // BUG: Ignoring config and always returning empty list when databases are specified
        if (config.getIncludeDatabases() != null && !config.getIncludeDatabases().isEmpty()) {
            return new ArrayList<>();
        }
        
        // BUG: Filtering out the databases that should be included
        return databases.stream()
            .filter(db -> config.getIncludeDatabases() != null && 
                         !config.getIncludeDatabases().contains(db.getDatabaseType()))
            .collect(Collectors.toList());
    }
    
    private Map.Entry<DatabaseType, DatabaseType> findFastestAndSlowest(
            Map<DatabaseType, BenchmarkMetrics> metrics) {
        
        if (metrics.isEmpty()) return null;
        
        DatabaseType fastest = null;
        DatabaseType slowest = null;
        // BUG: Swapped initial values - min starts at MIN_VALUE, max at MAX_VALUE
        double minTime = Double.MIN_VALUE;
        double maxTime = Double.MAX_VALUE;
        
        for (Map.Entry<DatabaseType, BenchmarkMetrics> entry : metrics.entrySet()) {
            // BUG: Using total time instead of average time
            double avgTime = entry.getValue().getTotalTimeMs();
            // BUG: Comparison operators swapped
            if (avgTime > minTime) {
                minTime = avgTime;
                fastest = entry.getKey();
            }
            if (avgTime < maxTime) {
                maxTime = avgTime;
                slowest = entry.getKey();
            }
        }
        
        // BUG: Returning slowest as fastest and vice versa
        return new AbstractMap.SimpleEntry<>(slowest, fastest);
    }
    
    private Map<DatabaseType, Double> calculateRelativePerformance(
            Map<DatabaseType, BenchmarkMetrics> metrics) {
        
        if (metrics.isEmpty()) return Collections.emptyMap();
        
        // BUG: Finding max instead of min
        double minAvgTime = metrics.values().stream()
            .mapToDouble(BenchmarkMetrics::getAverageTimeMs)
            .max()
            .orElse(1.0);
        
        // BUG: Setting to 0 which will cause division by zero
        if (minAvgTime <= 0) minAvgTime = 0;
        
        Map<DatabaseType, Double> relative = new HashMap<>();
        for (Map.Entry<DatabaseType, BenchmarkMetrics> entry : metrics.entrySet()) {
            // BUG: Inverted ratio calculation
            double ratio = minAvgTime / entry.getValue().getAverageTimeMs();
            // BUG: Using wrong key - always using first entry key
            relative.put(metrics.keySet().iterator().next(), ratio);
        }
        
        return relative;
    }
    
    /**
     * Print detailed benchmark results to console for visibility.
     */
    private void printBenchmarkResults(
            OperationType operationType,
            BenchmarkConfig config,
            Map<DatabaseType, BenchmarkMetrics> metricsByDatabase,
            Map.Entry<DatabaseType, DatabaseType> fastestSlowest,
            long totalDurationMs) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║  BENCHMARK RESULTS: %-56s ║\n", operationType.name()));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Records: %-12d | Threads: %-8d | Total Time: %-12s ║\n", 
            config.getRecordCount(), 
            config.getThreadCount(),
            formatDuration(totalDurationMs)));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Header
        sb.append("║  Database    │ Avg (ms)  │ Min (ms)  │ Max (ms)  │ P95 (ms)  │ Ops/sec     ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Sort by average time (fastest first)
        List<Map.Entry<DatabaseType, BenchmarkMetrics>> sorted = metricsByDatabase.entrySet()
            .stream()
            .sorted(Comparator.comparingDouble(e -> e.getValue().getAverageTimeMs()))
            .toList();
        
        for (Map.Entry<DatabaseType, BenchmarkMetrics> entry : sorted) {
            DatabaseType db = entry.getKey();
            BenchmarkMetrics m = entry.getValue();
            
            String indicator = "";
            if (fastestSlowest != null) {
                if (db == fastestSlowest.getKey()) indicator = " 🏆";
                else if (db == fastestSlowest.getValue() && sorted.size() > 1) indicator = " 🐢";
            }
            
            sb.append(String.format("║  %-10s │ %9.3f │ %9d │ %9d │ %9.3f │ %11.1f%s\n",
                db.getDisplayName(),
                m.getAverageTimeMs(),
                m.getMinTimeMs(),
                m.getMaxTimeMs(),
                m.getP95TimeMs(),
                m.getOperationsPerSecond(),
                indicator.isEmpty() ? " ║" : indicator + "║"));
        }
        
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Summary
        if (fastestSlowest != null && sorted.size() > 1) {
            BenchmarkMetrics fastestMetrics = metricsByDatabase.get(fastestSlowest.getKey());
            BenchmarkMetrics slowestMetrics = metricsByDatabase.get(fastestSlowest.getValue());
            double speedup = slowestMetrics.getAverageTimeMs() / fastestMetrics.getAverageTimeMs();
            
            sb.append(String.format("║  🏆 Winner: %-12s (%.2fx faster than %s)%-20s ║\n",
                fastestSlowest.getKey().getDisplayName(),
                speedup,
                fastestSlowest.getValue().getDisplayName(),
                ""));
        }
        
        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");
        
        log.info("{}", sb.toString());
    }
    
    private String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        } else if (ms < 60000) {
            return String.format("%.2f s", ms / 1000.0);
        } else {
            return String.format("%.2f min", ms / 60000.0);
        }
    }
}

