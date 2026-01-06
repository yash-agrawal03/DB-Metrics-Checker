package com.dbmetrics.redis.config;

import com.dbmetrics.common.model.BenchmarkRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for the benchmark module.
 */
@Configuration
@ConditionalOnProperty(name = "dbmetrics.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {
    
    public static final String BENCHMARK_KEY_PREFIX = "benchmark:";
    
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
    
    @Bean
    public RedisTemplate<String, BenchmarkRecord> benchmarkRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper) {
        
        RedisTemplate<String, BenchmarkRecord> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value serializer using Jackson
        Jackson2JsonRedisSerializer<BenchmarkRecord> serializer = 
            new Jackson2JsonRedisSerializer<>(redisObjectMapper, BenchmarkRecord.class);
        
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
}

