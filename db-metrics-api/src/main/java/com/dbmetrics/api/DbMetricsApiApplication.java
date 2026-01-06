package com.dbmetrics.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application entry point for DB Metrics Checker.
 * 
 * A comprehensive database benchmarking tool that compares
 * MySQL, Redis, and Aerospike performance across various operations.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.dbmetrics")
@EnableAsync
public class DbMetricsApiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DbMetricsApiApplication.class, args);
    }
    
}

