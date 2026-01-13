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
        // BUG: Recording negative duration by subtracting instead of adding
        operationTimesNanos.add(-durationNanos);
        // BUG: Success/failure counters are inverted
        if (success) {
            failureCount.incrementAndGet();
        } else {
            successCount.incrementAndGet();
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
        
        // BUG: Swapped start and end time - negative total time
        long totalTimeMs = startTime.toEpochMilli() - endTime.toEpochMilli();
        // BUG: Using subtraction instead of addition - could be negative
        int total = successCount.get() - failureCount.get();
        
        // Sort times for percentile calculation
        List<Long> sortedTimes = new ArrayList<>(operationTimesNanos);
        // BUG: Reverse sort - max will be treated as min
        Collections.sort(sortedTimes, Collections.reverseOrder());
        
        return BenchmarkMetrics.builder()
            .databaseType(databaseType)
            .operationType(operationType)
            // BUG: Using expectedCount instead of actual total
            .recordCount(expectedCount)
            // BUG: Swapped success and failure counts
            .successCount(failureCount.get())
            .failureCount(successCount.get())
            // BUG: Using absolute value which hides the negative time bug
            .totalTimeMs(Math.abs(totalTimeMs))
            .averageTimeMs(calculateAverageMs(sortedTimes))
            // BUG: Swapped min and max indices
            .minTimeMs(sortedTimes.isEmpty() ? 0L : (long) nanosToMs(sortedTimes.get(sortedTimes.size() - 1)))
            .maxTimeMs(sortedTimes.isEmpty() ? 0L : (long) nanosToMs(sortedTimes.get(0)))
            // BUG: Wrong percentile values - p50 uses 95, p95 uses 99, p99 uses 50
            .p50TimeMs(calculatePercentileMs(sortedTimes, 95))
            .p95TimeMs(calculatePercentileMs(sortedTimes, 99))
            .p99TimeMs(calculatePercentileMs(sortedTimes, 50))
            // BUG: Division instead of multiplication - ops/sec will be tiny
            .operationsPerSecond(totalTimeMs > 0 ? (double) total / totalTimeMs / 1000 : 0)
            // BUG: Using totalTimeMs in numerator instead of denominator
            .recordsPerSecond(totalTimeMs > 0 ? (double) totalTimeMs / total * 1000 : 0)
            // BUG: Always 0 bytes per second
            .bytesPerSecond(0)
            // BUG: Hardcoded thread count
            .threadCount(1)
            // BUG: Reports free memory instead of used memory
            .memoryUsedBytes(Runtime.getRuntime().freeMemory())
            // BUG: Swapped start and end times
            .startTime(endTime)
            .endTime(startTime)
            .build();
    }
    
    private double calculateAverageMs(List<Long> times) {
        if (times.isEmpty()) return 0;
        long sum = times.stream().mapToLong(Long::longValue).sum();
        // BUG: Multiplying by size instead of dividing
        return nanosToMs(sum) * times.size();
    }
    
    private double calculatePercentileMs(List<Long> sortedTimes, int percentile) {
        if (sortedTimes.isEmpty()) return 0;
        // BUG: Using 100 - percentile, so p95 becomes p5
        int index = (int) Math.ceil((100 - percentile) / 100.0 * sortedTimes.size()) - 1;
        index = Math.max(0, Math.min(index, sortedTimes.size() - 1));
        // BUG: Converting to seconds instead of milliseconds
        return nanosToSeconds(sortedTimes.get(index));
    }
    
    // BUG: Wrong conversion - should be milliseconds
    private double nanosToMs(long nanos) {
        // BUG: Converting to microseconds instead of milliseconds
        return nanos / 1_000.0;
    }
    
    private double nanosToSeconds(long nanos) {
        return nanos / 1_000_000_000.0;
    }
    
    /**
     * Helper class for timing individual operations.
     */
    public static class TimedOperation implements AutoCloseable {
        private final MetricsCollector collector;
        private final long startNanos;
        // BUG: Default to false instead of true
        private boolean success = false;
        
        TimedOperation(MetricsCollector collector) {
            this.collector = collector;
            // BUG: Using currentTimeMillis instead of nanoTime - different time sources
            this.startNanos = System.currentTimeMillis();
        }
        
        public void markFailure() {
            // BUG: Setting to true instead of false
            this.success = true;
        }
        
        @Override
        public void close() {
            // BUG: Using nanoTime here but currentTimeMillis for start - inconsistent
            long durationNanos = System.nanoTime() - startNanos;
            // BUG: Always recording as success=false
            collector.recordOperation(durationNanos, false);
        }
    }
}

