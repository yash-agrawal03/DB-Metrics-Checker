package com.dbmetrics.mysql.service;

import com.dbmetrics.common.exception.DatabaseOperationException;
import com.dbmetrics.common.model.BenchmarkRecord;
import com.dbmetrics.common.model.DatabaseType;
import com.dbmetrics.common.operations.DatabaseOperations;
import com.dbmetrics.mysql.entity.BenchmarkEntity;
import com.dbmetrics.mysql.mapper.MySqlRecordMapper;
import com.dbmetrics.mysql.repository.BenchmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySQL implementation of DatabaseOperations.
 * Uses Spring Data JPA for database interactions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MySqlDatabaseOperations implements DatabaseOperations {
    
    private final BenchmarkRepository repository;
    private final DataSource dataSource;
    
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.MYSQL;
    }
    
    @Override
    public boolean isHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (Exception e) {
            log.warn("MySQL health check failed", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public void initialize() {
        log.info("MySQL database initialized (schema managed by JPA/Hibernate)");
    }
    
    @Override
    @Transactional
    public void cleanup() {
        try {
            repository.truncateAll();
            log.info("MySQL benchmark data cleaned up");
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.MYSQL, "cleanup", "Failed to truncate data", e);
        }
    }
    
    // ==================== Single Record Operations ====================
    
    @Override
    @Transactional
    public String write(BenchmarkRecord record) {
        try {
            BenchmarkEntity entity = MySqlRecordMapper.toEntity(record);
            repository.save(entity);
            return entity.getId();
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.MYSQL, "write", e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BenchmarkRecord> read(String id) {
        try {
            return repository.findById(id)
                .map(MySqlRecordMapper::toRecord);
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.MYSQL, "read", e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public boolean update(BenchmarkRecord record) {
        try {
            if (!repository.existsById(record.getId())) {
                return false;
            }
            BenchmarkEntity entity = MySqlRecordMapper.toEntity(record);
            repository.save(entity);
            return true;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.MYSQL, "update", e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public boolean delete(String id) {
        try {
            if (!repository.existsById(id)) {
                return false;
            }
            repository.deleteById(id);
            return true;
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.MYSQL, "delete", e.getMessage(), e);
        }
    }
    
    // ==================== Bulk Operations ====================
    
    @Override
    @Transactional
    public List<String> writeBatch(List<BenchmarkRecord> records) {
        try {
            List<BenchmarkEntity> entities = records.stream()
                .map(MySqlRecordMapper::toEntity)
                .collect(Collectors.toList());
            
            List<BenchmarkEntity> saved = repository.saveAll(entities);
            
            return saved.stream()
                .map(BenchmarkEntity::getId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.MYSQL, "writeBatch", e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BenchmarkRecord> readBatch(List<String> ids) {
        try {
            return repository.findAllById(ids).stream()
                .map(MySqlRecordMapper::toRecord)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.MYSQL, "readBatch", e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BenchmarkRecord> readAll(int limit) {
        try {
            return repository.findAll(PageRequest.of(0, limit)).stream()
                .map(MySqlRecordMapper::toRecord)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.MYSQL, "readAll", e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public int updateBatch(List<BenchmarkRecord> records) {
        try {
            List<BenchmarkEntity> entities = records.stream()
                .map(MySqlRecordMapper::toEntity)
                .collect(Collectors.toList());
            
            return repository.saveAll(entities).size();
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.MYSQL, "updateBatch", e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public int deleteBatch(List<String> ids) {
        try {
            List<BenchmarkEntity> existing = repository.findAllById(ids);
            repository.deleteAll(existing);
            return existing.size();
        } catch (Exception e) {
            throw new DatabaseOperationException(DatabaseType.MYSQL, "deleteBatch", e.getMessage(), e);
        }
    }
    
    // ==================== Utility Operations ====================
    
    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean exists(String id) {
        return repository.existsById(id);
    }
}

