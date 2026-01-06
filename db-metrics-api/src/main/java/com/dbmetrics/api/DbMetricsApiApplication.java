package com.dbmetrics.api;

import hypertest.javaagent.HypertestAgent;
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
        String serviceIdentifier = "5c4be515-f188-49b7-a1e7-1c1f5c72174f";
        String exporterUrl = "https://logger.v3-app.hypertest.co";
        HypertestAgent.start(serviceIdentifier, "fk-db-metrics-checker", "API-KEY", exporterUrl, DbMetricsApiApplication.class);
        SpringApplication.run(DbMetricsApiApplication.class, args);

    }
    
}


