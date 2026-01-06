package com.dbmetrics.mysql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA Entity for storing benchmark records in MySQL.
 */
@Entity
@Table(name = "benchmark_records", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkEntity {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(length = 255)
    private String email;
    
    @Column(name = "phone_number", length = 50)
    private String phoneNumber;
    
    @Column(length = 500)
    private String address;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String country;
    
    @Column(length = 200)
    private String company;
    
    @Column(name = "job_title", length = 200)
    private String jobTitle;
    
    @Column(length = 100)
    private String department;
    
    @Column(precision = 12)
    private Double salary;
    
    private Integer age;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
}

