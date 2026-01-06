package com.dbmetrics.aerospike.mapper;

import com.aerospike.client.Bin;
import com.dbmetrics.common.model.BenchmarkRecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps between BenchmarkRecord and Aerospike bins.
 */
public final class AerospikeRecordMapper {
    
    // Bin names (Aerospike has a 15-character limit for bin names)
    public static final String BIN_ID = "id";
    public static final String BIN_FIRST_NAME = "first_name";
    public static final String BIN_LAST_NAME = "last_name";
    public static final String BIN_EMAIL = "email";
    public static final String BIN_PHONE = "phone";
    public static final String BIN_ADDRESS = "address";
    public static final String BIN_CITY = "city";
    public static final String BIN_COUNTRY = "country";
    public static final String BIN_COMPANY = "company";
    public static final String BIN_JOB_TITLE = "job_title";
    public static final String BIN_DEPARTMENT = "dept";
    public static final String BIN_SALARY = "salary";
    public static final String BIN_AGE = "age";
    public static final String BIN_IS_ACTIVE = "is_active";
    public static final String BIN_CREATED_AT = "created_at";
    public static final String BIN_UPDATED_AT = "updated_at";
    public static final String BIN_METADATA = "metadata";
    
    private AerospikeRecordMapper() {
        // Utility class
    }
    
    /**
     * Convert a BenchmarkRecord to Aerospike bins.
     */
    public static Bin[] toBins(BenchmarkRecord record) {
        List<Bin> bins = new ArrayList<>();
        
        bins.add(new Bin(BIN_ID, record.getId()));
        bins.add(new Bin(BIN_FIRST_NAME, record.getFirstName()));
        bins.add(new Bin(BIN_LAST_NAME, record.getLastName()));
        bins.add(new Bin(BIN_EMAIL, record.getEmail()));
        bins.add(new Bin(BIN_PHONE, record.getPhoneNumber()));
        bins.add(new Bin(BIN_ADDRESS, record.getAddress()));
        bins.add(new Bin(BIN_CITY, record.getCity()));
        bins.add(new Bin(BIN_COUNTRY, record.getCountry()));
        bins.add(new Bin(BIN_COMPANY, record.getCompany()));
        bins.add(new Bin(BIN_JOB_TITLE, record.getJobTitle()));
        bins.add(new Bin(BIN_DEPARTMENT, record.getDepartment()));
        
        if (record.getSalary() != null) {
            bins.add(new Bin(BIN_SALARY, record.getSalary()));
        }
        if (record.getAge() != null) {
            bins.add(new Bin(BIN_AGE, record.getAge()));
        }
        if (record.getIsActive() != null) {
            bins.add(new Bin(BIN_IS_ACTIVE, record.getIsActive()));
        }
        if (record.getCreatedAt() != null) {
            bins.add(new Bin(BIN_CREATED_AT, record.getCreatedAt().toEpochMilli()));
        }
        if (record.getUpdatedAt() != null) {
            bins.add(new Bin(BIN_UPDATED_AT, record.getUpdatedAt().toEpochMilli()));
        }
        if (record.getMetadata() != null) {
            bins.add(new Bin(BIN_METADATA, record.getMetadata()));
        }
        
        return bins.toArray(new Bin[0]);
    }
    
    /**
     * Convert an Aerospike record to a BenchmarkRecord.
     */
    public static BenchmarkRecord toRecord(String id, com.aerospike.client.Record record) {
        if (record == null) {
            return null;
        }
        
        Long createdAtValue = record.getLong(BIN_CREATED_AT);
        Long updatedAtValue = record.getLong(BIN_UPDATED_AT);
        
        return BenchmarkRecord.builder()
            .id(id)
            .firstName(record.getString(BIN_FIRST_NAME))
            .lastName(record.getString(BIN_LAST_NAME))
            .email(record.getString(BIN_EMAIL))
            .phoneNumber(record.getString(BIN_PHONE))
            .address(record.getString(BIN_ADDRESS))
            .city(record.getString(BIN_CITY))
            .country(record.getString(BIN_COUNTRY))
            .company(record.getString(BIN_COMPANY))
            .jobTitle(record.getString(BIN_JOB_TITLE))
            .department(record.getString(BIN_DEPARTMENT))
            .salary(record.getDouble(BIN_SALARY))
            .age(record.getInt(BIN_AGE))
            .isActive(record.getBoolean(BIN_IS_ACTIVE))
            .createdAt(toInstant(createdAtValue))
            .updatedAt(toInstant(updatedAtValue))
            .metadata(record.getString(BIN_METADATA))
            .build();
    }
    
    private static Instant toInstant(Long epochMilli) {
        return epochMilli != null && epochMilli > 0 
            ? Instant.ofEpochMilli(epochMilli) 
            : null;
    }
}

