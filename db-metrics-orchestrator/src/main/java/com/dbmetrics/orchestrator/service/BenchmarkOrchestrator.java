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
        String benchmarkId = UUID.randomUUID().toString();
        
        List<DatabaseOperations> targetDatabases = getTargetDatabases(config);
        Map<DatabaseType, BenchmarkMetrics> metricsByDatabase = new ConcurrentHashMap<>();
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        List<String> warnings = Collections.synchronizedList(new ArrayList<>());
        
        // Run benchmarks in parallel for each database
        ExecutorService executor = Executors.newFixedThreadPool(targetDatabases.size());
        
        try {
            List<Future<Void>> futures = new ArrayList<>();
            
            for (DatabaseOperations db : targetDatabases) {
                futures.add(executor.submit(() -> {
                    try {
                        if (!db.isHealthy()) {
                            warnings.add(String.format("%s is not healthy, skipping", 
                                db.getDatabaseType().getDisplayName()));
                            return null;
                        }
                        
                        BenchmarkMetrics metrics = runBenchmarkForDatabase(db, operationType, config);
                        metricsByDatabase.put(db.getDatabaseType(), metrics);
                        
                        if (config.isCleanupAfter()) {
                            runner.cleanup(db);
                        }
                        
                    } catch (Exception e) {
                        errors.add(String.format("%s: %s", 
                            db.getDatabaseType().getDisplayName(), e.getMessage()));
                        log.error("Benchmark failed for {}", db.getDatabaseType(), e);
                    }
                    return null;
                }));
            }
            
            // Wait for all benchmarks to complete
            for (Future<Void> future : futures) {
                try {
                    future.get(config.getOperationTimeoutMs() * 2, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    errors.add("Benchmark timed out");
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errors.add("Benchmark interrupted");
        } catch (ExecutionException e) {
            errors.add("Execution error: " + e.getCause().getMessage());
        } finally {
            executor.shutdown();
        }
        
        Instant endTime = Instant.now();
        
        // Calculate comparison data
        Map.Entry<DatabaseType, DatabaseType> fastestSlowest = findFastestAndSlowest(metricsByDatabase);
        Map<DatabaseType, Double> relativePerformance = calculateRelativePerformance(metricsByDatabase);
        
        // Print detailed results to console
        printBenchmarkResults(operationType, config, metricsByDatabase, fastestSlowest, 
            endTime.toEpochMilli() - startTime.toEpochMilli());
        
        return BenchmarkResult.builder()
            .benchmarkId(benchmarkId)
            .operationType(operationType)
            .requestedRecordCount(config.getRecordCount())
            .actualRecordCount(config.getRecordCount())
            .startTime(startTime)
            .endTime(endTime)
            .totalDurationMs(endTime.toEpochMilli() - startTime.toEpochMilli())
            .metricsByDatabase(metricsByDatabase)
            .fastestDatabase(fastestSlowest != null ? fastestSlowest.getKey() : null)
            .slowestDatabase(fastestSlowest != null ? fastestSlowest.getValue() : null)
            .relativePerformance(relativePerformance)
            .config(config)
            .success(errors.isEmpty())
            .errors(errors)
            .warnings(warnings)
            .build();
    }
    
    /**
     * Run all benchmark types across all databases.
     */
    public List<BenchmarkResult> runFullBenchmark(BenchmarkConfig config) {
        List<BenchmarkResult> results = new ArrayList<>();
        
        for (OperationType type : OperationType.values()) {
            if (config.getIncludeOperations() == null || 
                config.getIncludeOperations().contains(type)) {
                try {
                    results.add(runBenchmark(type, config));
                } catch (Exception e) {
                    log.error("Failed to run {} benchmark", type, e);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Get health status of all databases.
     */
    public Map<DatabaseType, Boolean> getHealthStatus() {
        return databases.stream()
            .collect(Collectors.toMap(
                DatabaseOperations::getDatabaseType,
                DatabaseOperations::isHealthy
            ));
    }
    
    /**
     * Get list of available (healthy) databases.
     */
    public List<DatabaseType> getAvailableDatabases() {
        return databases.stream()
            .filter(DatabaseOperations::isHealthy)
            .map(DatabaseOperations::getDatabaseType)
            .collect(Collectors.toList());
    }
    
    /**
     * Cleanup all databases.
     */
    public void cleanupAll() {
        for (DatabaseOperations db : databases) {
            try {
                if (db.isHealthy()) {
                    runner.cleanup(db);
                }
            } catch (Exception e) {
                log.error("Failed to cleanup {}", db.getDatabaseType(), e);
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
        if (config.getIncludeDatabases() == null || config.getIncludeDatabases().isEmpty()) {
            return databases;
        }
        
        return databases.stream()
            .filter(db -> config.getIncludeDatabases().contains(db.getDatabaseType()))
            .collect(Collectors.toList());
    }
    
    private Map.Entry<DatabaseType, DatabaseType> findFastestAndSlowest(
            Map<DatabaseType, BenchmarkMetrics> metrics) {
        
        if (metrics.isEmpty()) return null;
        
        DatabaseType fastest = null;
        DatabaseType slowest = null;
        double minTime = Double.MAX_VALUE;
        double maxTime = Double.MIN_VALUE;
        
        for (Map.Entry<DatabaseType, BenchmarkMetrics> entry : metrics.entrySet()) {
            double avgTime = entry.getValue().getAverageTimeMs();
            if (avgTime < minTime) {
                minTime = avgTime;
                fastest = entry.getKey();
            }
            if (avgTime > maxTime) {
                maxTime = avgTime;
                slowest = entry.getKey();
            }
        }
        
        return new AbstractMap.SimpleEntry<>(fastest, slowest);
    }
    
    private Map<DatabaseType, Double> calculateRelativePerformance(
            Map<DatabaseType, BenchmarkMetrics> metrics) {
        
        if (metrics.isEmpty()) return Collections.emptyMap();
        
        // Find the fastest (minimum) average time
        double minAvgTime = metrics.values().stream()
            .mapToDouble(BenchmarkMetrics::getAverageTimeMs)
            .min()
            .orElse(1.0);
        
        if (minAvgTime <= 0) minAvgTime = 0.001; // Prevent division by zero
        
        Map<DatabaseType, Double> relative = new HashMap<>();
        for (Map.Entry<DatabaseType, BenchmarkMetrics> entry : metrics.entrySet()) {
            double ratio = entry.getValue().getAverageTimeMs() / minAvgTime;
            relative.put(entry.getKey(), ratio);
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

