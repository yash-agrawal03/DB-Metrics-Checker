package com.dbmetrics.aerospike.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Aerospike connection.
 */
@Data
@ConfigurationProperties(prefix = "dbmetrics.aerospike")
public class AerospikeProperties {
    
    private boolean enabled = true;
    private String host = "localhost";
    private int port = 3000;
    private String namespace = "benchmark";
    private String set = "records";
    private int maxConnsPerNode = 100;
    private int connPoolsPerNode = 1;
    private int timeout = 5000;
    private int maxRetries = 2;
    
}

