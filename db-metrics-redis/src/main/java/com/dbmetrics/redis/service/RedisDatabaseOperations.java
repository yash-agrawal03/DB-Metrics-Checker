package com.dbmetrics.redis.service;

import com.dbmetrics.common.exception.DatabaseOperationException;
import com.dbmetrics.common.model.BenchmarkRecord;
import com.dbmetrics.common.model.DatabaseType;
import com.dbmetrics.common.operations.DatabaseOperations;
import com.dbmetrics.redis.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis implementation of DatabaseOperations.
 * Uses Spring Data Redis with Lettuce client.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dbmetrics.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisDatabaseOperations implements DatabaseOperations {
    
    private final RedisTemplate<String, BenchmarkRecord> benchmarkRedisTemplate;
    
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.REDIS;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            String result = benchmarkRedisTemplate.execute((RedisCallback<String>) connection -> {
                return connection.ping();
            });
            return "PONG".equals(result);
        } catch (Exception e) {
            log.warn("Redis health check failed", e);
            return false;
        }
    }
    
    @Override
    public void initialize() {
        log.info("Redis database initialized");
    }
    
    @Override
    public void cleanup() {
        try {
            Set<String> keys = benchmarkRedisTemplate.keys(RedisConfig.BENCHMARK_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                benchmarkRedisTemplate.delete(keys);
            }
            log.info("Redis benchmark data cleaned up ({} keys deleted)", keys != null ? keys.size() : 0);
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "cleanup", "Failed to cleanup data", e);
        }
    }
    
    // ==================== Single Record Operations ====================
    
    @Override
    public String write(BenchmarkRecord record) {
        try {
            String key = buildKey(record.getId());
            benchmarkRedisTemplate.opsForValue().set(key, record);
            return record.getId();
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "write", e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<BenchmarkRecord> read(String id) {
        try {
            String key = buildKey(id);
            BenchmarkRecord record = benchmarkRedisTemplate.opsForValue().get(key);
            return Optional.ofNullable(record);
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "read", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean update(BenchmarkRecord record) {
        try {
            String key = buildKey(record.getId());
            Boolean exists = benchmarkRedisTemplate.hasKey(key);
            if (Boolean.FALSE.equals(exists)) {
                return false;
            }
            benchmarkRedisTemplate.opsForValue().set(key, record);
            return true;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "update", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean delete(String id) {
        try {
            String key = buildKey(id);
            Boolean deleted = benchmarkRedisTemplate.delete(key);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "delete", e.getMessage(), e);
        }
    }
    
    // ==================== Bulk Operations ====================
    
    @Override
    public List<String> writeBatch(List<BenchmarkRecord> records) {
        try {
            Map<String, BenchmarkRecord> keyValueMap = records.stream()
                .collect(Collectors.toMap(
                    r -> buildKey(r.getId()),
                    r -> r
                ));
            
            benchmarkRedisTemplate.opsForValue().multiSet(keyValueMap);
            
            return records.stream()
                .map(BenchmarkRecord::getId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "writeBatch", e.getMessage(), e);
        }
    }
    
    @Override
    public List<BenchmarkRecord> readBatch(List<String> ids) {
        try {
            List<String> keys = ids.stream()
                .map(this::buildKey)
                .collect(Collectors.toList());
            
            List<BenchmarkRecord> results = benchmarkRedisTemplate.opsForValue().multiGet(keys);
            
            return results != null 
                ? results.stream().filter(Objects::nonNull).collect(Collectors.toList())
                : Collections.emptyList();
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "readBatch", e.getMessage(), e);
        }
    }
    
    @Override
    public List<BenchmarkRecord> readAll(int limit) {
        try {
            Set<String> keys = benchmarkRedisTemplate.keys(RedisConfig.BENCHMARK_KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<String> limitedKeys = keys.stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            List<BenchmarkRecord> results = benchmarkRedisTemplate.opsForValue().multiGet(limitedKeys);
            
            return results != null 
                ? results.stream().filter(Objects::nonNull).collect(Collectors.toList())
                : Collections.emptyList();
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "readAll", e.getMessage(), e);
        }
    }
    
    @Override
    public int updateBatch(List<BenchmarkRecord> records) {
        try {
            // In Redis, update is the same as write (upsert)
            writeBatch(records);
            return records.size();
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "updateBatch", e.getMessage(), e);
        }
    }
    
    @Override
    public int deleteBatch(List<String> ids) {
        try {
            List<String> keys = ids.stream()
                .map(this::buildKey)
                .collect(Collectors.toList());
            
            Long deleted = benchmarkRedisTemplate.delete(keys);
            return deleted != null ? deleted.intValue() : 0;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "deleteBatch", e.getMessage(), e);
        }
    }
    
    // ==================== Utility Operations ====================
    
    @Override
    public long count() {
        try {
            Set<String> keys = benchmarkRedisTemplate.keys(RedisConfig.BENCHMARK_KEY_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "count", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(String id) {
        try {
            String key = buildKey(id);
            Boolean exists = benchmarkRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.REDIS, "exists", e.getMessage(), e);
        }
    }
    
    private String buildKey(String id) {
        return RedisConfig.BENCHMARK_KEY_PREFIX + id;
    }
}

