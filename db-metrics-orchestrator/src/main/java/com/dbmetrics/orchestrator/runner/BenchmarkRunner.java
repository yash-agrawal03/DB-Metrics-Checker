package com.dbmetrics.orchestrator.runner;

import com.dbmetrics.common.generator.MockDataGenerator;
import com.dbmetrics.common.metrics.MetricsCollector;
import com.dbmetrics.common.model.*;
import com.dbmetrics.common.operations.DatabaseOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Executes benchmark operations for a single database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BenchmarkRunner {
    
    /**
     * Run a write benchmark on the specified database.
     */
    public BenchmarkMetrics runWriteBenchmark(DatabaseOperations db, BenchmarkConfig config) {
        log.info("Starting write benchmark for {} with {} records", 
            db.getDatabaseType(), config.getRecordCount());
        
        List<BenchmarkRecord> records = MockDataGenerator.generateRecords(config.getRecordCount());
        MetricsCollector collector = new MetricsCollector(
            db.getDatabaseType(), OperationType.WRITE, config.getRecordCount());
        
        // Warmup
        if (config.isWarmupEnabled()) {
            runWarmup(db, config.getWarmupIterations());
        }
        
        collector.start();
        
        for (BenchmarkRecord record : records) {
            try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                db.write(record);
            } catch (Exception e) {
                log.warn("Write failed for record {}: {}", record.getId(), e.getMessage());
            }
        }
        
        collector.stop();
        
        BenchmarkMetrics metrics = collector.buildMetrics();
        log.info("Write benchmark completed: {}", metrics.getSummary());
        
        return metrics;
    }
    
    /**
     * Run a read benchmark on the specified database.
     */
    public BenchmarkMetrics runReadBenchmark(DatabaseOperations db, BenchmarkConfig config) {
        log.info("Starting read benchmark for {} with {} records", 
            db.getDatabaseType(), config.getRecordCount());
        
        // First, ensure we have data to read
        List<BenchmarkRecord> records = MockDataGenerator.generateRecords(config.getRecordCount());
        List<String> ids = db.writeBatch(records);
        
        MetricsCollector collector = new MetricsCollector(
            db.getDatabaseType(), OperationType.READ, config.getRecordCount());
        
        collector.start();
        
        for (String id : ids) {
            try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                db.read(id);
            } catch (Exception e) {
                log.warn("Read failed for id {}: {}", id, e.getMessage());
            }
        }
        
        collector.stop();
        
        BenchmarkMetrics metrics = collector.buildMetrics();
        log.info("Read benchmark completed: {}", metrics.getSummary());
        
        return metrics;
    }
    
    /**
     * Run a bulk write benchmark on the specified database.
     */
    public BenchmarkMetrics runBulkWriteBenchmark(DatabaseOperations db, BenchmarkConfig config) {
        log.info("Starting bulk write benchmark for {} with {} records (batch size: {})", 
            db.getDatabaseType(), config.getRecordCount(), config.getBatchSize());
        
        List<BenchmarkRecord> allRecords = MockDataGenerator.generateRecords(config.getRecordCount());
        MetricsCollector collector = new MetricsCollector(
            db.getDatabaseType(), OperationType.BULK_WRITE, config.getRecordCount());
        
        // Split into batches
        List<List<BenchmarkRecord>> batches = partition(allRecords, config.getBatchSize());
        
        collector.start();
        
        for (List<BenchmarkRecord> batch : batches) {
            try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                db.writeBatch(batch);
            } catch (Exception e) {
                log.warn("Bulk write failed for batch: {}", e.getMessage());
            }
        }
        
        collector.stop();
        
        BenchmarkMetrics metrics = collector.buildMetrics();
        log.info("Bulk write benchmark completed: {}", metrics.getSummary());
        
        return metrics;
    }
    
    /**
     * Run a bulk read benchmark on the specified database.
     */
    public BenchmarkMetrics runBulkReadBenchmark(DatabaseOperations db, BenchmarkConfig config) {
        log.info("Starting bulk read benchmark for {} with {} records (batch size: {})", 
            db.getDatabaseType(), config.getRecordCount(), config.getBatchSize());
        
        // First, ensure we have data to read
        List<BenchmarkRecord> records = MockDataGenerator.generateRecords(config.getRecordCount());
        List<String> ids = db.writeBatch(records);
        
        MetricsCollector collector = new MetricsCollector(
            db.getDatabaseType(), OperationType.BULK_READ, config.getRecordCount());
        
        // Split into batches
        List<List<String>> batches = partition(ids, config.getBatchSize());
        
        collector.start();
        
        for (List<String> batch : batches) {
            try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                db.readBatch(batch);
            } catch (Exception e) {
                log.warn("Bulk read failed for batch: {}", e.getMessage());
            }
        }
        
        collector.stop();
        
        BenchmarkMetrics metrics = collector.buildMetrics();
        log.info("Bulk read benchmark completed: {}", metrics.getSummary());
        
        return metrics;
    }
    
    /**
     * Run concurrent read/write benchmark on the specified database.
     */
    public BenchmarkMetrics runConcurrentBenchmark(DatabaseOperations db, BenchmarkConfig config) {
        log.info("Starting concurrent R/W benchmark for {} with {} records ({} threads)", 
            db.getDatabaseType(), config.getRecordCount(), config.getThreadCount());
        
        List<BenchmarkRecord> records = MockDataGenerator.generateRecords(config.getRecordCount());
        MetricsCollector collector = new MetricsCollector(
            db.getDatabaseType(), OperationType.CONCURRENT_READ_WRITE, config.getRecordCount() * 2);
        collector.setThreadCount(config.getThreadCount());
        
        ExecutorService executor = Executors.newFixedThreadPool(config.getThreadCount());
        
        try {
            // Write half the data first for reads
            int halfCount = config.getRecordCount() / 2;
            List<String> existingIds = db.writeBatch(records.subList(0, halfCount));
            List<BenchmarkRecord> newRecords = records.subList(halfCount, records.size());
            
            List<Callable<Void>> tasks = new ArrayList<>();
            
            // Create read tasks
            for (String id : existingIds) {
                tasks.add(() -> {
                    try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                        db.read(id);
                    }
                    return null;
                });
            }
            
            // Create write tasks
            for (BenchmarkRecord record : newRecords) {
                tasks.add(() -> {
                    try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                        db.write(record);
                    }
                    return null;
                });
            }
            
            collector.start();
            
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    log.warn("Concurrent operation failed: {}", e.getCause().getMessage());
                }
            }
            
            collector.stop();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Concurrent benchmark interrupted", e);
        } finally {
            executor.shutdown();
        }
        
        BenchmarkMetrics metrics = collector.buildMetrics();
        log.info("Concurrent benchmark completed: {}", metrics.getSummary());
        
        return metrics;
    }
    
    /**
     * Cleanup benchmark data from the database.
     */
    public void cleanup(DatabaseOperations db) {
        log.info("Cleaning up benchmark data for {}", db.getDatabaseType());
        db.cleanup();
    }
    
    private void runWarmup(DatabaseOperations db, int iterations) {
        log.debug("Running {} warmup iterations for {}", iterations, db.getDatabaseType());
        List<BenchmarkRecord> warmupRecords = MockDataGenerator.generateRecords(iterations);
        
        for (BenchmarkRecord record : warmupRecords) {
            try {
                String id = db.write(record);
                db.read(id);
                db.delete(id);
            } catch (Exception e) {
                // Ignore warmup errors
            }
        }
    }
    
    private <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }
}

