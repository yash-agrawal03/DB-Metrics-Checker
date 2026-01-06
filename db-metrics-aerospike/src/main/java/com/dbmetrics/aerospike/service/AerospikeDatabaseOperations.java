package com.dbmetrics.aerospike.service;

import com.aerospike.client.*;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.dbmetrics.aerospike.config.AerospikeProperties;
import com.dbmetrics.aerospike.mapper.AerospikeRecordMapper;
import com.dbmetrics.common.exception.DatabaseOperationException;
import com.dbmetrics.common.model.BenchmarkRecord;
import com.dbmetrics.common.model.DatabaseType;
import com.dbmetrics.common.operations.DatabaseOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Aerospike implementation of DatabaseOperations.
 * Uses the native Aerospike Java client for maximum performance.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "dbmetrics.aerospike.enabled", havingValue = "true", matchIfMissing = true)
public class AerospikeDatabaseOperations implements DatabaseOperations {
    
    private final AerospikeClient client;
    private final AerospikeProperties properties;
    private final WritePolicy writePolicy;
    private final Policy readPolicy;
    private final BatchPolicy batchPolicy;
    
    public AerospikeDatabaseOperations(
            AerospikeClient client,
            AerospikeProperties properties,
            @Qualifier("aerospikeWritePolicy") WritePolicy writePolicy,
            @Qualifier("aerospikeReadPolicy") Policy readPolicy,
            @Qualifier("aerospikeBatchPolicy") BatchPolicy batchPolicy) {
        this.client = client;
        this.properties = properties;
        this.writePolicy = writePolicy;
        this.readPolicy = readPolicy;
        this.batchPolicy = batchPolicy;
    }
    
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.AEROSPIKE;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            return client.isConnected();
        } catch (Exception e) {
            log.warn("Aerospike health check failed", e);
            return false;
        }
    }
    
    @Override
    public void initialize() {
        log.info("Aerospike database initialized (namespace: {}, set: {})", 
            properties.getNamespace(), properties.getSet());
    }
    
    @Override
    public void cleanup() {
        try {
            ScanPolicy scanPolicy = new ScanPolicy();
            scanPolicy.includeBinData = false;
            
            AtomicLong deleted = new AtomicLong(0);
            
            client.scanAll(scanPolicy, properties.getNamespace(), properties.getSet(),
                (key, record) -> {
                    try {
                        client.delete(writePolicy, key);
                        deleted.incrementAndGet();
                    } catch (AerospikeException e) {
                        log.warn("Failed to delete key during cleanup: {}", key, e);
                    }
                });
            
            log.info("Aerospike benchmark data cleaned up ({} records deleted)", deleted.get());
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "cleanup", "Failed to cleanup data", e);
        }
    }
    
    // ==================== Single Record Operations ====================
    
    @Override
    public String write(BenchmarkRecord record) {
        try {
            Key key = buildKey(record.getId());
            Bin[] bins = AerospikeRecordMapper.toBins(record);
            client.put(writePolicy, key, bins);
            return record.getId();
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "write", e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<BenchmarkRecord> read(String id) {
        try {
            Key key = buildKey(id);
            com.aerospike.client.Record record = client.get(readPolicy, key);
            return Optional.ofNullable(AerospikeRecordMapper.toRecord(id, record));
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "read", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean update(BenchmarkRecord record) {
        try {
            Key key = buildKey(record.getId());
            if (!client.exists(readPolicy, key)) {
                return false;
            }
            Bin[] bins = AerospikeRecordMapper.toBins(record);
            client.put(writePolicy, key, bins);
            return true;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "update", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean delete(String id) {
        try {
            Key key = buildKey(id);
            return client.delete(writePolicy, key);
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "delete", e.getMessage(), e);
        }
    }
    
    // ==================== Bulk Operations ====================
    
    @Override
    public List<String> writeBatch(List<BenchmarkRecord> records) {
        List<String> writtenIds = new ArrayList<>();
        
        try {
            for (BenchmarkRecord record : records) {
                try {
                    write(record);
                    writtenIds.add(record.getId());
                } catch (Exception e) {
                    log.warn("Failed to write record {} in batch", record.getId(), e);
                }
            }
            return writtenIds;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "writeBatch", e.getMessage(), e);
        }
    }
    
    @Override
    public List<BenchmarkRecord> readBatch(List<String> ids) {
        try {
            Key[] keys = ids.stream()
                .map(this::buildKey)
                .toArray(Key[]::new);
            
            com.aerospike.client.Record[] records = client.get(batchPolicy, keys);
            
            List<BenchmarkRecord> results = new ArrayList<>();
            for (int i = 0; i < records.length; i++) {
                if (records[i] != null) {
                    results.add(AerospikeRecordMapper.toRecord(ids.get(i), records[i]));
                }
            }
            return results;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "readBatch", e.getMessage(), e);
        }
    }
    
    @Override
    public List<BenchmarkRecord> readAll(int limit) {
        try {
            List<BenchmarkRecord> results = new ArrayList<>();
            ScanPolicy scanPolicy = new ScanPolicy();
            scanPolicy.maxRecords = limit;
            
            client.scanAll(scanPolicy, properties.getNamespace(), properties.getSet(),
                (key, record) -> {
                    if (results.size() < limit) {
                        String id = key.userKey != null ? key.userKey.toString() : key.digest.toString();
                        results.add(AerospikeRecordMapper.toRecord(id, record));
                    }
                });
            
            return results;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "readAll", e.getMessage(), e);
        }
    }
    
    @Override
    public int updateBatch(List<BenchmarkRecord> records) {
        int updated = 0;
        try {
            for (BenchmarkRecord record : records) {
                if (update(record)) {
                    updated++;
                }
            }
            return updated;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "updateBatch", e.getMessage(), e);
        }
    }
    
    @Override
    public int deleteBatch(List<String> ids) {
        int deleted = 0;
        try {
            for (String id : ids) {
                if (delete(id)) {
                    deleted++;
                }
            }
            return deleted;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "deleteBatch", e.getMessage(), e);
        }
    }
    
    // ==================== Utility Operations ====================
    
    @Override
    public long count() {
        try {
            AtomicLong counter = new AtomicLong(0);
            ScanPolicy scanPolicy = new ScanPolicy();
            scanPolicy.includeBinData = false;
            
            client.scanAll(scanPolicy, properties.getNamespace(), properties.getSet(),
                (key, record) -> counter.incrementAndGet());
            
            return counter.get();
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "count", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(String id) {
        try {
            Key key = buildKey(id);
            return client.exists(readPolicy, key);
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.AEROSPIKE, "exists", e.getMessage(), e);
        }
    }
    
    private Key buildKey(String id) {
        return new Key(properties.getNamespace(), properties.getSet(), id);
    }
}

