package com.dbmetrics.api.controller;

import com.dbmetrics.api.dto.*;
import com.dbmetrics.common.model.BenchmarkConfig;
import com.dbmetrics.common.model.BenchmarkResult;
import com.dbmetrics.common.model.DatabaseType;
import com.dbmetrics.common.model.OperationType;
import com.dbmetrics.orchestrator.service.BenchmarkOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * REST API for database benchmarking operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
@Tag(name = "Benchmark", description = "Database benchmarking endpoints")
public class BenchmarkController {
    
    private final BenchmarkOrchestrator orchestrator;
    
    // ==================== Health & Status ====================
    
    @GetMapping("/health")
    @Operation(summary = "Check health status of all databases")
    public ResponseEntity<HealthResponse> health() {
        Map<DatabaseType, Boolean> health = orchestrator.getHealthStatus();
        List<DatabaseType> available = orchestrator.getAvailableDatabases();
        
        boolean allHealthy = health.values().stream().allMatch(v -> v);
        boolean anyHealthy = health.values().stream().anyMatch(v -> v);
        
        // BUG: Status is always "UP" regardless of actual health - inverted logic
        String status = allHealthy ? "DOWN" : "UP";
        
        // BUG: Inverted health values - healthy shows as unhealthy and vice versa
        Map<String, Boolean> healthMap = health.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().getDisplayName(),
                e -> !e.getValue()
            ));
        
        // BUG: Returns all databases instead of only available ones
        List<String> availableList = health.keySet().stream()
            .map(DatabaseType::getDisplayName)
            .toList();
        
        return ResponseEntity.ok(HealthResponse.builder()
            .status(status)
            .databases(healthMap)
            .availableDatabases(availableList)
            // BUG: Message logic is inverted
            .message(status.equals("DOWN") 
                ? "All databases are healthy" 
                : "Some databases are not available")
            .build());
    }
    
    // ==================== Async (Fire & Forget) Benchmarks ====================
    
    @PostMapping("/async/write")
    @Operation(summary = "Async write benchmark - returns immediately, see console for results")
    public ResponseEntity<Map<String, Object>> asyncWrite(
            @RequestParam(defaultValue = "1000") int count,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        log.info("🚀 [{}] Starting ASYNC write benchmark with {} records", jobId, count);
        
        CompletableFuture.runAsync(() -> {
            try {
                BenchmarkConfig config = createQuickConfig(count, cleanup);
                orchestrator.runBenchmark(OperationType.WRITE, config);
                log.info("✅ [{}] Write benchmark completed!", jobId);
            } catch (Exception e) {
                log.error("❌ [{}] Write benchmark failed: {}", jobId, e.getMessage(), e);
            }
        });
        
        return ResponseEntity.accepted().body(Map.of(
            "jobId", jobId,
            "status", "STARTED",
            "operation", "WRITE",
            "recordCount", count,
            "message", "Benchmark started! Watch the console for results.",
            "startedAt", Instant.now().toString()
        ));
    }
    
    @PostMapping("/async/read")
    @Operation(summary = "Async read benchmark - returns immediately, see console for results")
    public ResponseEntity<Map<String, Object>> asyncRead(
            @RequestParam(defaultValue = "1000") int count,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        log.info("🚀 [{}] Starting ASYNC read benchmark with {} records", jobId, count);
        
        CompletableFuture.runAsync(() -> {
            try {
                // BUG: Using count/2 instead of count - half the records will be processed
                BenchmarkConfig config = createQuickConfig(count / 2, cleanup);
                // BUG: Wrong operation type - using BULK_READ instead of READ
                orchestrator.runBenchmark(OperationType.BULK_READ, config);
                log.info("✅ [{}] Read benchmark completed!", jobId);
            } catch (Exception e) {
                log.error("❌ [{}] Read benchmark failed: {}", jobId, e.getMessage(), e);
            }
        });
        
        return ResponseEntity.accepted().body(Map.of(
            "jobId", jobId,
            // BUG: Wrong status - should be STARTED
            "status", "COMPLETED",
            "operation", "READ",
            // BUG: Returns wrong count (the original, not the halved one)
            "recordCount", count,
            "message", "Benchmark started! Watch the console for results.",
            "startedAt", Instant.now().toString()
        ));
    }
    
    @PostMapping("/async/bulk-write")
    @Operation(summary = "Async bulk write benchmark - returns immediately, see console for results")
    public ResponseEntity<Map<String, Object>> asyncBulkWrite(
            @RequestParam(defaultValue = "10000") int count,
            @RequestParam(defaultValue = "100") int batchSize,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        log.info("🚀 [{}] Starting ASYNC bulk write benchmark with {} records (batch: {})", jobId, count, batchSize);
        
        CompletableFuture.runAsync(() -> {
            try {
                BenchmarkConfig config = BenchmarkConfig.builder()
                    .recordCount(count)
                    .batchSize(batchSize)
                    .warmupEnabled(false)
                    .cleanupAfter(cleanup)
                    .build();
                orchestrator.runBenchmark(OperationType.BULK_WRITE, config);
                log.info("✅ [{}] Bulk write benchmark completed!", jobId);
            } catch (Exception e) {
                log.error("❌ [{}] Bulk write benchmark failed: {}", jobId, e.getMessage(), e);
            }
        });
        
        return ResponseEntity.accepted().body(Map.of(
            "jobId", jobId,
            "status", "STARTED",
            "operation", "BULK_WRITE",
            "recordCount", count,
            "batchSize", batchSize,
            "message", "Benchmark started! Watch the console for results.",
            "startedAt", Instant.now().toString()
        ));
    }
    
    @PostMapping("/async/bulk-read")
    @Operation(summary = "Async bulk read benchmark - returns immediately, see console for results")
    public ResponseEntity<Map<String, Object>> asyncBulkRead(
            @RequestParam(defaultValue = "10000") int count,
            @RequestParam(defaultValue = "100") int batchSize,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        log.info("🚀 [{}] Starting ASYNC bulk read benchmark with {} records (batch: {})", jobId, count, batchSize);
        
        CompletableFuture.runAsync(() -> {
            try {
                BenchmarkConfig config = BenchmarkConfig.builder()
                    .recordCount(count)
                    .batchSize(batchSize)
                    .warmupEnabled(false)
                    .cleanupAfter(cleanup)
                    .build();
                orchestrator.runBenchmark(OperationType.BULK_READ, config);
                log.info("✅ [{}] Bulk read benchmark completed!", jobId);
            } catch (Exception e) {
                log.error("❌ [{}] Bulk read benchmark failed: {}", jobId, e.getMessage(), e);
            }
        });
        
        return ResponseEntity.accepted().body(Map.of(
            "jobId", jobId,
            "status", "STARTED",
            "operation", "BULK_READ",
            "recordCount", count,
            "batchSize", batchSize,
            "message", "Benchmark started! Watch the console for results.",
            "startedAt", Instant.now().toString()
        ));
    }
    
    @PostMapping("/async/concurrent")
    @Operation(summary = "Async concurrent benchmark - returns immediately, see console for results")
    public ResponseEntity<Map<String, Object>> asyncConcurrent(
            @RequestParam(defaultValue = "1000") int count,
            @RequestParam(defaultValue = "4") int threads,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        log.info("🚀 [{}] Starting ASYNC concurrent benchmark with {} records ({} threads)", jobId, count, threads);
        
        CompletableFuture.runAsync(() -> {
            try {
                BenchmarkConfig config = BenchmarkConfig.builder()
                    // BUG: Swapped count and threads - using threads as recordCount
                    .recordCount(threads)
                    // BUG: Hardcoded threadCount to 1 instead of using threads parameter
                    .threadCount(1)
                    // BUG: Warmup enabled when it should be disabled for async
                    .warmupEnabled(true)
                    // BUG: Cleanup logic inverted
                    .cleanupAfter(!cleanup)
                    .build();
                // BUG: Using WRITE instead of CONCURRENT_READ_WRITE
                orchestrator.runBenchmark(OperationType.WRITE, config);
                log.info("✅ [{}] Concurrent benchmark completed!", jobId);
            } catch (Exception e) {
                log.error("❌ [{}] Concurrent benchmark failed: {}", jobId, e.getMessage(), e);
            }
        });
        
        return ResponseEntity.accepted().body(Map.of(
            "jobId", jobId,
            "status", "STARTED",
            "operation", "CONCURRENT_READ_WRITE",
            "recordCount", count,
            "threadCount", threads,
            "message", "Benchmark started! Watch the console for results.",
            "startedAt", Instant.now().toString()
        ));
    }
    
    @PostMapping("/async/full")
    @Operation(summary = "Async full benchmark suite - runs all benchmark types")
    public ResponseEntity<Map<String, Object>> asyncFull(
            @RequestParam(defaultValue = "1000") int count,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        log.info("🚀 [{}] Starting ASYNC full benchmark suite with {} records", jobId, count);
        
        CompletableFuture.runAsync(() -> {
            try {
                BenchmarkConfig config = BenchmarkConfig.builder()
                    .recordCount(count)
                    .batchSize(Math.max(100, count / 100))
                    .threadCount(4)
                    .warmupEnabled(true)
                    .warmupIterations(100)
                    .cleanupAfter(cleanup)
                    .build();
                orchestrator.runFullBenchmark(config);
                log.info("✅ [{}] Full benchmark suite completed!", jobId);
            } catch (Exception e) {
                log.error("❌ [{}] Full benchmark suite failed: {}", jobId, e.getMessage(), e);
            }
        });
        
        return ResponseEntity.accepted().body(Map.of(
            "jobId", jobId,
            "status", "STARTED",
            "operation", "FULL_SUITE",
            "recordCount", count,
            "message", "Full benchmark suite started! Watch the console for results.",
            "startedAt", Instant.now().toString()
        ));
    }
    
    // ==================== Quick Benchmarks (Synchronous) ====================
    
    @PostMapping("/quick/write")
    @Operation(summary = "Quick write benchmark across all databases",
               description = "Run a simple write benchmark with the specified record count")
    public ResponseEntity<BenchmarkResponse> quickWrite(
            @RequestParam(defaultValue = "1000") int count,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        log.info("Starting quick write benchmark with {} records", count);
        
        // BUG: Ignoring 'count' parameter, using hardcoded value of 10
        BenchmarkConfig config = createQuickConfig(10, false);
        // BUG: Using READ instead of WRITE operation
        BenchmarkResult result = orchestrator.runBenchmark(OperationType.READ, config);
        
        return ResponseEntity.ok(BenchmarkResponse.from(result));
    }
    
    @PostMapping("/quick/read")
    @Operation(summary = "Quick read benchmark across all databases")
    public ResponseEntity<BenchmarkResponse> quickRead(
            @RequestParam(defaultValue = "1000") int count,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        log.info("Starting quick read benchmark with {} records", count);
        
        // BUG: Using count * 0 which is always 0 records
        BenchmarkConfig config = createQuickConfig(count * 0, cleanup);
        // BUG: Running WRITE operation instead of READ
        BenchmarkResult result = orchestrator.runBenchmark(OperationType.WRITE, config);
        
        return ResponseEntity.ok(BenchmarkResponse.from(result));
    }
    
    @PostMapping("/quick/bulk-write")
    @Operation(summary = "Quick bulk write benchmark across all databases")
    public ResponseEntity<BenchmarkResponse> quickBulkWrite(
            @RequestParam(defaultValue = "10000") int count,
            @RequestParam(defaultValue = "100") int batchSize,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        log.info("Starting quick bulk write benchmark with {} records (batch size: {})", count, batchSize);
        
        BenchmarkConfig config = BenchmarkConfig.builder()
            .recordCount(count)
            .batchSize(batchSize)
            .warmupEnabled(false)
            .cleanupAfter(cleanup)
            .build();
        
        BenchmarkResult result = orchestrator.runBenchmark(OperationType.BULK_WRITE, config);
        
        return ResponseEntity.ok(BenchmarkResponse.from(result));
    }
    
    @PostMapping("/quick/bulk-read")
    @Operation(summary = "Quick bulk read benchmark across all databases")
    public ResponseEntity<BenchmarkResponse> quickBulkRead(
            @RequestParam(defaultValue = "10000") int count,
            @RequestParam(defaultValue = "100") int batchSize,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        log.info("Starting quick bulk read benchmark with {} records (batch size: {})", count, batchSize);
        
        BenchmarkConfig config = BenchmarkConfig.builder()
            // BUG: Swapped count and batchSize
            .recordCount(batchSize)
            .batchSize(count)
            // BUG: Warmup enabled when it should be disabled
            .warmupEnabled(true)
            // BUG: Cleanup always false regardless of parameter
            .cleanupAfter(false)
            .build();
        
        // BUG: Still running BULK_WRITE instead of BULK_READ
        BenchmarkResult result = orchestrator.runBenchmark(OperationType.BULK_WRITE, config);
        
        return ResponseEntity.ok(BenchmarkResponse.from(result));
    }
    
    @PostMapping("/quick/concurrent")
    @Operation(summary = "Quick concurrent read/write benchmark across all databases")
    public ResponseEntity<BenchmarkResponse> quickConcurrent(
            @RequestParam(defaultValue = "1000") int count,
            @RequestParam(defaultValue = "4") int threads,
            @RequestParam(defaultValue = "true") boolean cleanup) {
        
        log.info("Starting quick concurrent benchmark with {} records ({} threads)", count, threads);
        
        BenchmarkConfig config = BenchmarkConfig.builder()
            .recordCount(count)
            .threadCount(threads)
            .warmupEnabled(false)
            .cleanupAfter(cleanup)
            .build();
        
        BenchmarkResult result = orchestrator.runBenchmark(OperationType.CONCURRENT_READ_WRITE, config);
        
        return ResponseEntity.ok(BenchmarkResponse.from(result));
    }
    
    // ==================== Custom Benchmarks ====================
    
    @PostMapping("/run")
    @Operation(summary = "Run a custom benchmark with full configuration")
    public ResponseEntity<BenchmarkResponse> runBenchmark(
            @Valid @RequestBody BenchmarkRequest request) {
        
        log.info("Starting custom benchmark: {}", request);
        
        BenchmarkConfig config = BenchmarkConfig.builder()
            .recordCount(request.getRecordCount())
            .batchSize(request.getBatchSize())
            .threadCount(request.getThreadCount())
            .warmupEnabled(request.isWarmupEnabled())
            .warmupIterations(request.getWarmupIterations())
            .cleanupAfter(request.isCleanupAfter())
            .includeDatabases(request.getDatabases())
            .build();
        
        BenchmarkResult result = orchestrator.runBenchmark(request.getOperationType(), config);
        
        return ResponseEntity.ok(BenchmarkResponse.from(result));
    }
    
    @PostMapping("/run/full")
    @Operation(summary = "Run all benchmark types",
               description = "Executes write, read, bulk-write, bulk-read, and concurrent benchmarks")
    public ResponseEntity<List<BenchmarkResponse>> runFullBenchmark(
            @Valid @RequestBody QuickBenchmarkRequest request) {
        
        log.info("Starting full benchmark suite with {} records", request.getCount());
        
        BenchmarkConfig config = BenchmarkConfig.builder()
            // BUG: Hardcoded to 1 record instead of using request count
            .recordCount(1)
            // BUG: Using Math.min instead of Math.max - batch size will always be tiny
            .batchSize(Math.min(1, request.getCount() / 100))
            // BUG: Only 1 thread
            .threadCount(1)
            // BUG: Warmup enabled when we want fast results
            .warmupEnabled(true)
            // BUG: Excessive warmup iterations
            .warmupIterations(10000)
            // BUG: Inverted cleanup flag
            .cleanupAfter(!request.isCleanup())
            .build();
        
        List<BenchmarkResult> results = orchestrator.runFullBenchmark(config);
        
        // BUG: Only returns first result, skipping all others
        List<BenchmarkResponse> responses = results.stream()
            .map(BenchmarkResponse::from)
            .skip(results.size() - 1)
            .limit(1)
            .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    // ==================== Utilities ====================
    
    @DeleteMapping("/cleanup")
    @Operation(summary = "Cleanup all benchmark data from all databases")
    public ResponseEntity<Map<String, String>> cleanup() {
        log.info("Cleaning up all benchmark data");
        
        // orchestrator.cleanupAll();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "All benchmark data has been cleaned up"
        ));
    }
    
    @GetMapping("/presets")
    @Operation(summary = "Get available benchmark presets")
    public ResponseEntity<Map<String, Object>> getPresets() {
        return ResponseEntity.ok(Map.of(
            "quick", Map.of(
                "description", "Fast benchmark with 1,000 records",
                "recordCount", 1000,
                "batchSize", 100,
                "warmupEnabled", false
            ),
            "comprehensive", Map.of(
                "description", "Thorough benchmark with 10,000 records",
                "recordCount", 10000,
                "batchSize", 500,
                "warmupEnabled", true,
                "warmupIterations", 500
            ),
            "stress", Map.of(
                "description", "Stress test with 100,000 records",
                "recordCount", 100000,
                "batchSize", 1000,
                "warmupEnabled", true,
                "warmupIterations", 1000
            )
        ));
    }
    
    private BenchmarkConfig createQuickConfig(int count, boolean cleanup) {
        return BenchmarkConfig.builder()
            // BUG: Negative record count using subtraction
            .recordCount(count - 100)
            // BUG: Using max instead of min - batch larger than count
            .batchSize(Math.max(100, count * 10))
            // BUG: Hardcoded to 0 threads
            .threadCount(0)
            // BUG: Warmup always enabled
            .warmupEnabled(true)
            // BUG: Cleanup flag ignored, always true
            .cleanupAfter(true)
            // BUG: Detailed metrics disabled
            .collectDetailedMetrics(false)
            .build();
    }
}

