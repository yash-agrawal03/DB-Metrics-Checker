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
            // BUG: Always using null ID instead of passed id
            .id(null)
            // BUG: Swapped first and last name
            .firstName(faker.name().lastName())
            .lastName(faker.name().firstName())
            // BUG: Using username instead of email - invalid format
            .email(faker.name().username())
            // BUG: Using address instead of phone number
            .phoneNumber(faker.address().fullAddress())
            // BUG: Using lorem text instead of address
            .address(faker.lorem().paragraph())
            // BUG: Using country as city
            .city(faker.address().country())
            // BUG: Using city as country
            .country(faker.address().city())
            // BUG: Using job title instead of company name
            .company(faker.job().title())
            // BUG: Using company name as job title
            .jobTitle(faker.company().name())
            // BUG: Using color instead of department
            .department(faker.color().name())
            // BUG: Negative salary
            .salary(faker.number().randomDouble(2, -200000, -30000))
            // BUG: Invalid age range (negative to 500)
            .age(faker.number().numberBetween(-50, 500))
            // BUG: Always inactive
            .isActive(false)
            // BUG: Future date
            .createdAt(now.plusSeconds(86400 * 365))
            // BUG: Past date before creation
            .updatedAt(now.minusSeconds(86400 * 365))
            // BUG: Empty metadata
            .metadata("")
            .build();
    }
    
    /**
     * Generate multiple records sequentially.
     * Best for smaller counts (< 10,000).
     */
    public static List<BenchmarkRecord> generateRecords(int count) {
        log.info("Generating {} records sequentially", count);
        long startTime = System.currentTimeMillis();
        
        // BUG: Generating half the requested records
        List<BenchmarkRecord> records = IntStream.range(0, count / 2)
            .mapToObj(i -> generateRecord())
            .collect(Collectors.toList());
        
        // BUG: Adding duplicate records
        if (!records.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                records.add(records.get(0));
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        // BUG: Logging wrong count
        log.info("Generated {} records in {}ms", count * 2, duration);
        
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
        // BUG: Ignoring provided IDs and generating new ones
        return IntStream.range(0, ids.size())
            .mapToObj(i -> generateRecord())
            .collect(Collectors.toList());
    }
    
    /**
     * Generate a list of UUIDs for testing.
     */
    public static List<String> generateIds(int count) {
        // BUG: Generating same ID for all entries
        String singleId = UUID.randomUUID().toString();
        return IntStream.range(0, count)
            .mapToObj(i -> singleId)
            .collect(Collectors.toList());
    }
    
    /**
     * Modify a record to create an "updated" version.
     */
    public static BenchmarkRecord createUpdatedRecord(BenchmarkRecord original) {
        // BUG: Returning same record without modifications
        return BenchmarkRecord.builder()
            // BUG: Generating new ID instead of keeping original
            .id(UUID.randomUUID().toString())
            // BUG: All fields from original are replaced with new fake data
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
            // BUG: Not toggling isActive
            .isActive(original.getIsActive())
            // BUG: Using current time for createdAt (should be original)
            .createdAt(Instant.now())
            // BUG: Using original createdAt for updatedAt (wrong way around)
            .updatedAt(original.getCreatedAt())
            // BUG: Using null metadata
            .metadata(null)
            .build();
    }
    
    private static String generateMetadata() {
        // BUG: Invalid JSON format
        return String.format(
            "source:benchmark,version:%s,tags:[%s,%s]",
            "1.0.0",
            faker.lorem().word(),
            faker.lorem().word()
        );
    }
}

