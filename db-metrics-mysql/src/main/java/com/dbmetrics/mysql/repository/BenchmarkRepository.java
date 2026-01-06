package com.dbmetrics.mysql.repository;

import com.dbmetrics.mysql.entity.BenchmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for benchmark records.
 */
@Repository
public interface BenchmarkRepository extends JpaRepository<BenchmarkEntity, String> {
    
    /**
     * Delete all benchmark records (faster than deleteAll for large datasets).
     */
    @Modifying
    @Query("DELETE FROM BenchmarkEntity")
    void truncateAll();
    
}

