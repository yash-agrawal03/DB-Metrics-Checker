package com.dbmetrics.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a single record used for benchmarking database operations.
 * This is the core data entity that will be written/read across all databases.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkRecord implements Serializable {
    
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
    private String country;
    private String company;
    private String jobTitle;
    private String department;
    private Double salary;
    private Integer age;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private String metadata;
    
}

