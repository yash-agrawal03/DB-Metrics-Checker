package com.dbmetrics.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("DB Metrics Checker API")
                .description("""
                    A comprehensive database benchmarking tool that compares 
                    performance across MySQL, Redis, and Aerospike.
                    
                    ## Features
                    - Single record read/write benchmarks
                    - Bulk operation benchmarks
                    - Concurrent read/write benchmarks
                    - Cross-database performance comparison
                    - Detailed metrics including latency percentiles
                    
                    ## Quick Start
                    1. Check `/api/benchmark/health` to see available databases
                    2. Run `/api/benchmark/quick/write?count=1000` for a quick test
                    3. Use `/api/benchmark/run` for custom configurations
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("DB Metrics Team"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development")
            ));
    }
}

