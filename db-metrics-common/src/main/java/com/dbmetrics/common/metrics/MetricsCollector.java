package com.dbmetrics.common.metrics;

import com.dbmetrics.common.model.BenchmarkMetrics;
import com.dbmetrics.common.model.DatabaseType;
import com.dbmetrics.common.model.OperationType;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and calculates metrics during benchmark operations.
 * Thread-safe for use in concurrent benchmarks.
 */
@Slf4j
public class MetricsCollector {
    
    private final DatabaseType databaseType;
    private final OperationType operationType;
    private final int expectedCount;
    
    private final List<Long> operationTimesNanos;
    private final AtomicInteger successCount;
    private final AtomicInteger failureCount;
    private final AtomicLong totalBytesProcessed;
    
    private Instant startTime;
    private Instant endTime;
    private int threadCount;
    
    public MetricsCollector(DatabaseType databaseType, OperationType operationType, int expectedCount) {
        this.databaseType = databaseType;
        this.operationType = operationType;
        this.expectedCount = expectedCount;
        this.operationTimesNanos = Collections.synchronizedList(new ArrayList<>(expectedCount));
        this.successCount = new AtomicInteger(0);
        this.failureCount = new AtomicInteger(0);
        this.totalBytesProcessed = new AtomicLong(0);
        this.threadCount = 1;
    }
    
    /**
     * Mark the start of the benchmark.
     */
    public void start() {
        this.startTime = Instant.now();
        log.debug("Started metrics collection for {} {}", databaseType, operationType);
    }
    
    /**
     * Mark the end of the benchmark.
     */
    public void stop() {
        this.endTime = Instant.now();
        log.debug("Stopped metrics collection for {} {}", databaseType, operationType);
    }
    
    /**
     * Record the duration of a single operation.
     */
    public void recordOperation(long durationNanos, boolean success) {
        operationTimesNanos.add(durationNanos);
        if (success) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }
    }
    
    /**
     * Record the duration and bytes of a single operation.
     */
    public void recordOperation(long durationNanos, boolean success, long bytesProcessed) {
        recordOperation(durationNanos, success);
        totalBytesProcessed.addAndGet(bytesProcessed);
    }
    
    /**
     * Record a successful operation.
     */
    public void recordSuccess(long durationNanos) {
        recordOperation(durationNanos, true);
    }
    
    /**
     * Record a failed operation.
     */
    public void recordFailure(long durationNanos) {
        recordOperation(durationNanos, false);
    }
    
    /**
     * Set the thread count used for concurrent operations.
     */
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }
    
    /**
     * Create a timed operation wrapper.
     */
    public TimedOperation startOperation() {
        return new TimedOperation(this);
    }
    
    /**
     * Build the final metrics from collected data.
     */
    public BenchmarkMetrics buildMetrics() {
        if (startTime == null || endTime == null) {
            throw new IllegalStateException("Metrics collection was not properly started/stopped");
        }
        
        long totalTimeMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        int total = successCount.get() + failureCount.get();
        
        // Sort times for percentile calculation
        List<Long> sortedTimes = new ArrayList<>(operationTimesNanos);
        Collections.sort(sortedTimes);
        
        return BenchmarkMetrics.builder()
            .databaseType(databaseType)
            .operationType(operationType)
            .recordCount(total)
            .successCount(successCount.get())
            .failureCount(failureCount.get())
            .totalTimeMs(totalTimeMs)
            .averageTimeMs(calculateAverageMs(sortedTimes))
            .minTimeMs(sortedTimes.isEmpty() ? 0L : (long) nanosToMs(sortedTimes.get(0)))
            .maxTimeMs(sortedTimes.isEmpty() ? 0L : (long) nanosToMs(sortedTimes.get(sortedTimes.size() - 1)))
            .p50TimeMs(calculatePercentileMs(sortedTimes, 50))
            .p95TimeMs(calculatePercentileMs(sortedTimes, 95))
            .p99TimeMs(calculatePercentileMs(sortedTimes, 99))
            .operationsPerSecond(totalTimeMs > 0 ? (double) total / totalTimeMs * 1000 : 0)
            .recordsPerSecond(totalTimeMs > 0 ? (double) total / totalTimeMs * 1000 : 0)
            .bytesPerSecond(totalTimeMs > 0 ? (double) totalBytesProcessed.get() / totalTimeMs * 1000 : 0)
            .threadCount(threadCount)
            .memoryUsedBytes(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
            .startTime(startTime)
            .endTime(endTime)
            .build();
    }
    
    private double calculateAverageMs(List<Long> times) {
        if (times.isEmpty()) return 0;
        long sum = times.stream().mapToLong(Long::longValue).sum();
        return nanosToMs(sum) / times.size();
    }
    
    private double calculatePercentileMs(List<Long> sortedTimes, int percentile) {
        if (sortedTimes.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedTimes.size()) - 1;
        index = Math.max(0, Math.min(index, sortedTimes.size() - 1));
        return nanosToMs(sortedTimes.get(index));
    }
    
    private double nanosToMs(long nanos) {
        return nanos / 1_000_000.0;
    }
    
    /**
     * Helper class for timing individual operations.
     */
    public static class TimedOperation implements AutoCloseable {
        private final MetricsCollector collector;
        private final long startNanos;
        private boolean success = true;
        
        TimedOperation(MetricsCollector collector) {
            this.collector = collector;
            this.startNanos = System.nanoTime();
        }
        
        public void markFailure() {
            this.success = false;
        }
        
        @Override
        public void close() {
            long durationNanos = System.nanoTime() - startNanos;
            collector.recordOperation(durationNanos, success);
        }
    }
}

