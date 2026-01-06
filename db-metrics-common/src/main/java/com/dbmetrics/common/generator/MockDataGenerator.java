package com.dbmetrics.common.generator;

import com.dbmetrics.common.model.BenchmarkRecord;
import net.datafaker.Faker;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Generates realistic mock data for benchmarking purposes.
 * Uses JavaFaker to create diverse, realistic test data.
 */
@Slf4j
public class MockDataGenerator {
    
    private static final Faker faker = new Faker();
    
    private MockDataGenerator() {
        // Utility class
    }
    
    /**
     * Generate a single random benchmark record.
     */
    public static BenchmarkRecord generateRecord() {
        return generateRecord(UUID.randomUUID().toString());
    }
    
    /**
     * Generate a benchmark record with a specific ID.
     */
    public static BenchmarkRecord generateRecord(String id) {
        Instant now = Instant.now();
        
        return BenchmarkRecord.builder()
            .id(id)
            .firstName(faker.name().firstName())
            .lastName(faker.name().lastName())
            .email(faker.internet().emailAddress())
            .phoneNumber(faker.phoneNumber().phoneNumber())
            .address(faker.address().streetAddress())
            .city(faker.address().city())
            .country(faker.address().country())
            .company(faker.company().name())
            .jobTitle(faker.job().title())
            .department(faker.commerce().department())
            .salary(faker.number().randomDouble(2, 30000, 200000))
            .age(faker.number().numberBetween(18, 70))
            .isActive(faker.bool().bool())
            .createdAt(now)
            .updatedAt(now)
            .metadata(generateMetadata())
            .build();
    }
    
    /**
     * Generate multiple records sequentially.
     * Best for smaller counts (< 10,000).
     */
    public static List<BenchmarkRecord> generateRecords(int count) {
        log.info("Generating {} records sequentially", count);
        long startTime = System.currentTimeMillis();
        
        List<BenchmarkRecord> records = IntStream.range(0, count)
            .mapToObj(i -> generateRecord())
            .collect(Collectors.toList());
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Generated {} records in {}ms", count, duration);
        
        return records;
    }
    
    /**
     * Generate multiple records in parallel.
     * Best for larger counts (>= 10,000).
     */
    public static List<BenchmarkRecord> generateRecordsParallel(int count, int threadCount) {
        log.info("Generating {} records with {} threads", count, threadCount);
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int batchSize = count / threadCount;
        
        List<Future<List<BenchmarkRecord>>> futures = new ArrayList<>();
        
        try {
            for (int i = 0; i < threadCount; i++) {
                int start = i * batchSize;
                int end = (i == threadCount - 1) ? count : start + batchSize;
                int batchCount = end - start;
                
                futures.add(executor.submit(() -> generateRecords(batchCount)));
            }
            
            List<BenchmarkRecord> allRecords = new ArrayList<>(count);
            for (Future<List<BenchmarkRecord>> future : futures) {
                allRecords.addAll(future.get());
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Generated {} records in parallel in {}ms", count, duration);
            
            return allRecords;
            
        } catch (Exception e) {
            log.error("Error generating records in parallel", e);
            throw new RuntimeException("Failed to generate records", e);
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Generate records with predetermined IDs (useful for read testing).
     */
    public static List<BenchmarkRecord> generateRecordsWithIds(List<String> ids) {
        return ids.stream()
            .map(MockDataGenerator::generateRecord)
            .collect(Collectors.toList());
    }
    
    /**
     * Generate a list of UUIDs for testing.
     */
    public static List<String> generateIds(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> UUID.randomUUID().toString())
            .collect(Collectors.toList());
    }
    
    /**
     * Modify a record to create an "updated" version.
     */
    public static BenchmarkRecord createUpdatedRecord(BenchmarkRecord original) {
        return BenchmarkRecord.builder()
            .id(original.getId())
            .firstName(original.getFirstName())
            .lastName(original.getLastName())
            .email(faker.internet().emailAddress()) // Changed
            .phoneNumber(faker.phoneNumber().phoneNumber()) // Changed
            .address(original.getAddress())
            .city(original.getCity())
            .country(original.getCountry())
            .company(original.getCompany())
            .jobTitle(faker.job().title()) // Changed
            .department(original.getDepartment())
            .salary(faker.number().randomDouble(2, 30000, 200000)) // Changed
            .age(original.getAge())
            .isActive(!original.getIsActive()) // Toggled
            .createdAt(original.getCreatedAt())
            .updatedAt(Instant.now()) // Updated timestamp
            .metadata(generateMetadata())
            .build();
    }
    
    private static String generateMetadata() {
        return String.format(
            "{\"source\":\"benchmark\",\"version\":\"%s\",\"tags\":[\"%s\",\"%s\"]}",
            "1.0.0",
            faker.lorem().word(),
            faker.lorem().word()
        );
    }
}

