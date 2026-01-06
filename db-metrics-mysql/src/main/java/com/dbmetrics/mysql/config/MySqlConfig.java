package com.dbmetrics.mysql.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * MySQL configuration for the benchmark module.
 */
@Configuration
@ConditionalOnProperty(name = "dbmetrics.mysql.enabled", havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(basePackages = "com.dbmetrics.mysql.repository")
@EntityScan(basePackages = "com.dbmetrics.mysql.entity")
public class MySqlConfig {
    
}

