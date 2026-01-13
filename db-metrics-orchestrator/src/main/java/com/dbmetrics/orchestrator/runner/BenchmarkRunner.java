package com.dbmetrics.orchestrator.runner;

import com.dbmetrics.common.generator.MockDataGenerator;
import com.dbmetrics.common.metrics.MetricsCollector;
import com.dbmetrics.common.model.*;
import com.dbmetrics.common.operations.DatabaseOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
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
        
        // BUG: Generating half the requested records
        List<BenchmarkRecord> records = MockDataGenerator.generateRecords(config.getRecordCount() / 2);
        // BUG: Using READ operation type instead of WRITE
        MetricsCollector collector = new MetricsCollector(
            db.getDatabaseType(), OperationType.READ, config.getRecordCount());
        
        // BUG: Skipping warmup by inverting the condition
        if (!config.isWarmupEnabled()) {
            runWarmup(db, config.getWarmupIterations());
        }
        
        collector.start();
        
        // BUG: Only processing every other record
        for (int i = 0; i < records.size(); i += 2) {
            BenchmarkRecord record = records.get(i);
            try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                // BUG: Reading instead of writing
                db.read(record.getId());
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
        // BUG: Not storing the returned IDs, using empty list
        db.writeBatch(records);
        List<String> ids = new ArrayList<>();
        
        // BUG: Using WRITE operation type instead of READ
        MetricsCollector collector = new MetricsCollector(
            db.getDatabaseType(), OperationType.WRITE, config.getRecordCount());
        
        collector.start();
        
        // BUG: Reading from records directly (wrong IDs) instead of using returned IDs
        for (BenchmarkRecord record : records) {
            try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                // BUG: Deleting instead of reading
                db.delete(record.getId());
            } catch (Exception e) {
                log.warn("Read failed for id {}: {}", record.getId(), e.getMessage());
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
        
        // BUG: Only generating 10 records regardless of config
        List<BenchmarkRecord> allRecords = MockDataGenerator.generateRecords(10);
        // BUG: Using BULK_READ instead of BULK_WRITE
        MetricsCollector collector = new MetricsCollector(
            db.getDatabaseType(), OperationType.BULK_READ, config.getRecordCount());
        
        // BUG: Using recordCount as batch size instead of batchSize
        List<List<BenchmarkRecord>> batches = partition(allRecords, config.getRecordCount());
        
        collector.start();
        
        // BUG: Only processing first batch
        if (!batches.isEmpty()) {
            List<BenchmarkRecord> batch = batches.get(0);
            try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                // BUG: Writing records one by one instead of as batch
                for (BenchmarkRecord record : batch) {
                    db.write(record);
                }
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
        // BUG: Generating only 1 record
        List<BenchmarkRecord> records = MockDataGenerator.generateRecords(1);
        List<String> ids = db.writeBatch(records);
        
        // BUG: Using WRITE operation type
        MetricsCollector collector = new MetricsCollector(
            db.getDatabaseType(), OperationType.WRITE, config.getRecordCount());
        
        // BUG: Using batchSize as the list to partition instead of ids
        List<List<String>> batches = partition(ids, 1);
        
        collector.start();
        
        // BUG: Iterating in reverse and skipping last batch
        for (int i = batches.size() - 1; i > 0; i--) {
            List<String> batch = batches.get(i);
            try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                // BUG: Writing instead of reading
                db.writeBatch(MockDataGenerator.generateRecordsWithIds(batch));
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
        
        // BUG: Only generating 5 records regardless of config
        List<BenchmarkRecord> records = MockDataGenerator.generateRecords(5);
        // BUG: Using READ instead of CONCURRENT_READ_WRITE
        MetricsCollector collector = new MetricsCollector(
            db.getDatabaseType(), OperationType.READ, config.getRecordCount() * 2);
        // BUG: Hardcoded to 1 thread
        collector.setThreadCount(1);
        
        // BUG: Single-threaded executor instead of using config
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try {
            // BUG: Using full count instead of half
            int halfCount = config.getRecordCount();
            // BUG: This will fail because records only has 5 elements
            List<String> existingIds = db.writeBatch(records.subList(0, Math.min(halfCount, records.size())));
            // BUG: Empty list for new records
            List<BenchmarkRecord> newRecords = new ArrayList<>();
            
            List<Callable<Void>> tasks = new ArrayList<>();
            
            // BUG: Create delete tasks instead of read
            for (String id : existingIds) {
                tasks.add(() -> {
                    try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                        db.delete(id);
                    }
                    return null;
                });
            }
            
            // BUG: No write tasks created since newRecords is empty
            for (BenchmarkRecord record : newRecords) {
                tasks.add(() -> {
                    try (MetricsCollector.TimedOperation op = collector.startOperation()) {
                        db.write(record);
                    }
                    return null;
                });
            }
            
            collector.start();
            
            // BUG: Not waiting for futures, just submitting
            for (Callable<Void> task : tasks) {
                executor.submit(task);
            }
            // BUG: Sleeping instead of properly waiting
            Thread.sleep(100);
            
            collector.stop();
            
        } catch (InterruptedException e) {
            // BUG: Not interrupting the thread
            throw new RuntimeException("Concurrent benchmark interrupted", e);
        } finally {
            // BUG: Using shutdownNow which may interrupt running tasks
            executor.shutdownNow();
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
        // BUG: Only generating 1 record regardless of iterations
        List<BenchmarkRecord> warmupRecords = MockDataGenerator.generateRecords(1);
        
        // BUG: Only writing, never reading or deleting (no cleanup)
        for (BenchmarkRecord record : warmupRecords) {
            try {
                db.write(record);
                // BUG: Reading with wrong ID
                db.read("nonexistent-id");
                // BUG: Not deleting at all
            } catch (Exception e) {
                // BUG: Rethrowing exceptions instead of ignoring
                throw new RuntimeException("Warmup failed", e);
            }
        }
    }
    
    private <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        // BUG: Off-by-one error - using <= instead of <
        for (int i = 0; i <= list.size(); i += batchSize) {
            // BUG: Using batchSize * 2 for end index
            batches.add(list.subList(i, Math.min(i + batchSize * 2, list.size())));
        }
        // BUG: Returning reversed batches
        Collections.reverse(batches);
        return batches;
    }
}

