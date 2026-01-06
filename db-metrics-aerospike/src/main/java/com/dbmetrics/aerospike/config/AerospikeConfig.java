package com.dbmetrics.aerospike.config;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.BatchPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aerospike configuration for the benchmark module.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "dbmetrics.aerospike.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AerospikeProperties.class)
public class AerospikeConfig {
    
    @Bean(destroyMethod = "close")
    public AerospikeClient aerospikeClient(AerospikeProperties properties) {
        ClientPolicy policy = new ClientPolicy();
        policy.maxConnsPerNode = properties.getMaxConnsPerNode();
        policy.connPoolsPerNode = properties.getConnPoolsPerNode();
        policy.timeout = properties.getTimeout();
        policy.failIfNotConnected = false; // Allow graceful degradation
        
        Host host = new Host(properties.getHost(), properties.getPort());
        
        try {
            AerospikeClient client = new AerospikeClient(policy, host);
            log.info("Connected to Aerospike at {}:{}", properties.getHost(), properties.getPort());
            return client;
        } catch (Exception e) {
            log.warn("Could not connect to Aerospike at {}:{} - {}", 
                properties.getHost(), properties.getPort(), e.getMessage());
            // Return a client anyway - operations will fail gracefully
            return new AerospikeClient(policy, host);
        }
    }
    
    @Bean
    public WritePolicy aerospikeWritePolicy(AerospikeProperties properties) {
        WritePolicy policy = new WritePolicy();
        policy.totalTimeout = properties.getTimeout();
        policy.maxRetries = properties.getMaxRetries();
        policy.sendKey = true; // Store the key with the record
        return policy;
    }
    
    @Bean
    public Policy aerospikeReadPolicy(AerospikeProperties properties) {
        Policy policy = new Policy();
        policy.totalTimeout = properties.getTimeout();
        policy.maxRetries = properties.getMaxRetries();
        return policy;
    }
    
    @Bean
    public BatchPolicy aerospikeBatchPolicy(AerospikeProperties properties) {
        BatchPolicy policy = new BatchPolicy();
        policy.totalTimeout = properties.getTimeout() * 2; // More time for batch
        policy.maxRetries = properties.getMaxRetries();
        return policy;
    }
}

