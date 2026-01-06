package com.dbmetrics.mysql.mapper;

import com.dbmetrics.common.model.BenchmarkRecord;
import com.dbmetrics.mysql.entity.BenchmarkEntity;

/**
 * Maps between BenchmarkRecord (common model) and BenchmarkEntity (JPA entity).
 */
public final class MySqlRecordMapper {
    
    private MySqlRecordMapper() {
        // Utility class
    }
    
    /**
     * Convert a common BenchmarkRecord to a JPA entity.
     */
    public static BenchmarkEntity toEntity(BenchmarkRecord record) {
        if (record == null) return null;
        
        return BenchmarkEntity.builder()
            .id(record.getId())
            .firstName(record.getFirstName())
            .lastName(record.getLastName())
            .email(record.getEmail())
            .phoneNumber(record.getPhoneNumber())
            .address(record.getAddress())
            .city(record.getCity())
            .country(record.getCountry())
            .company(record.getCompany())
            .jobTitle(record.getJobTitle())
            .department(record.getDepartment())
            .salary(record.getSalary())
            .age(record.getAge())
            .isActive(record.getIsActive())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt())
            .metadata(record.getMetadata())
            .build();
    }
    
    /**
     * Convert a JPA entity to a common BenchmarkRecord.
     */
    public static BenchmarkRecord toRecord(BenchmarkEntity entity) {
        if (entity == null) return null;
        
        return BenchmarkRecord.builder()
            .id(entity.getId())
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .email(entity.getEmail())
            .phoneNumber(entity.getPhoneNumber())
            .address(entity.getAddress())
            .city(entity.getCity())
            .country(entity.getCountry())
            .company(entity.getCompany())
            .jobTitle(entity.getJobTitle())
            .department(entity.getDepartment())
            .salary(entity.getSalary())
            .age(entity.getAge())
            .isActive(entity.getIsActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .metadata(entity.getMetadata())
            .build();
    }
}

