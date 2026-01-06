# 🚀 DB Metrics Checker

A comprehensive **multi-database benchmarking tool** that compares performance across **MySQL**, **Redis**, and **Aerospike**. Built with Spring Boot, this tool helps you make data-driven decisions about database selection for your use cases.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [API Reference](#-api-reference)
- [Test Data](#-test-data)
- [Use Cases](#-use-cases)
- [Configuration](#-configuration)
- [Project Structure](#-project-structure)

---

## ✨ Features

### Benchmark Operations
| Operation | Description |
|-----------|-------------|
| **Write** | Individual INSERT operations - tests single record write latency |
| **Read** | Individual SELECT/GET operations - tests single record read latency |
| **Bulk Write** | Batch INSERT operations - tests throughput for high-volume writes |
| **Bulk Read** | Batch SELECT/MGET operations - tests throughput for batch reads |
| **Concurrent** | Mixed read/write with multiple threads - simulates real-world load |

### Metrics Collected
- ⏱️ **Latency**: Average, Min, Max, P50, P95, P99
- 📊 **Throughput**: Operations per second
- ✅ **Reliability**: Success/failure rates
- 🏆 **Comparison**: Relative performance across databases

### Async Support
- **Fire & Forget** endpoints - API returns immediately
- Results displayed in console with beautiful formatting
- No HTTP timeout issues for long-running benchmarks

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        DB Metrics Checker                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────┐    ┌──────────────────┐    ┌────────────────┐  │
│  │   REST API  │───▶│   Orchestrator   │───▶│ Benchmark      │  │
│  │  (db-api)   │    │                  │    │ Runner         │  │
│  └─────────────┘    └──────────────────┘    └────────────────┘  │
│                              │                       │           │
│         ┌────────────────────┼───────────────────────┘           │
│         ▼                    ▼                    ▼              │
│  ┌────────────┐      ┌────────────┐      ┌────────────┐         │
│  │   MySQL    │      │   Redis    │      │ Aerospike  │         │
│  │  Module    │      │  Module    │      │  Module    │         │
│  └────────────┘      └────────────┘      └────────────┘         │
│         │                    │                    │              │
└─────────┼────────────────────┼────────────────────┼──────────────┘
          ▼                    ▼                    ▼
    ┌──────────┐         ┌──────────┐         ┌──────────┐
    │  MySQL   │         │  Redis   │         │Aerospike │
    │  8.0+    │         │  7.0+    │         │  6.0+    │
    └──────────┘         └──────────┘         └──────────┘
```

### Module Structure

| Module | Description |
|--------|-------------|
| `db-metrics-common` | Shared models, interfaces, utilities |
| `db-metrics-mysql` | MySQL implementation using Spring Data JPA |
| `db-metrics-redis` | Redis implementation using Spring Data Redis |
| `db-metrics-aerospike` | Aerospike implementation using native client |
| `db-metrics-orchestrator` | Benchmark coordination and metrics collection |
| `db-metrics-api` | REST API endpoints and Swagger UI |

---

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. Clone the Repository

```bash
git clone https://github.com/yash-agrawal03/DB-Metrics-Checker.git
cd DB-Metrics-Checker
```

### 2. Start the Databases

```bash
docker-compose up -d
```

This starts:
- **MySQL** on port `3306`
- **Redis** on port `6379`
- **Aerospike** on port `3000`

Wait ~30 seconds for databases to initialize, then verify:

```bash
docker-compose ps
```

### 3. Build the Project

```bash
./mvnw clean install -DskipTests
```

### 4. Run the Application

```bash
./mvnw spring-boot:run -pl db-metrics-api
```

### 5. Verify Everything Works

```bash
curl http://localhost:8080/api/benchmark/health
```

Expected response:
```json
{
  "status": "UP",
  "databases": {
    "MySQL": true,
    "Redis": true,
    "Aerospike": true
  },
  "availableDatabases": ["MySQL", "Redis", "Aerospike"],
  "message": "All databases are healthy"
}
```

### 6. Run Your First Benchmark

```bash
# Fire & forget - results appear in console
curl -X POST "http://localhost:8080/api/benchmark/async/bulk-write?count=1000"
```

---

## 📚 API Reference

### Base URL
```
http://localhost:8080/api/benchmark
```

### Swagger UI
```
http://localhost:8080/swagger-ui.html
```

---

### 🔥 Async Endpoints (Recommended)

These return immediately - **watch the console for results!**

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/async/write` | POST | Async single writes |
| `/async/read` | POST | Async single reads |
| `/async/bulk-write` | POST | Async batch writes |
| `/async/bulk-read` | POST | Async batch reads |
| `/async/concurrent` | POST | Async concurrent test |
| `/async/full` | POST | Run ALL benchmark types |

#### Examples:

```bash
# Write 1,000 records
curl -X POST "http://localhost:8080/api/benchmark/async/write?count=1000"

# Bulk write 10,000 records in batches of 100
curl -X POST "http://localhost:8080/api/benchmark/async/bulk-write?count=10000&batchSize=100"

# Bulk write 100,000 records (stress test)
curl -X POST "http://localhost:8080/api/benchmark/async/bulk-write?count=100000&batchSize=500"

# Concurrent test with 8 threads
curl -X POST "http://localhost:8080/api/benchmark/async/concurrent?count=5000&threads=8"

# Full benchmark suite
curl -X POST "http://localhost:8080/api/benchmark/async/full?count=1000"
```

#### Immediate Response:
```json
{
  "jobId": "a1b2c3d4",
  "status": "STARTED",
  "operation": "BULK_WRITE",
  "recordCount": 10000,
  "message": "Benchmark started! Watch the console for results.",
  "startedAt": "2026-01-06T08:30:00.000Z"
}
```

---

### ⚡ Synchronous Endpoints

These wait for completion and return full results (may timeout for large datasets).

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Check database connectivity |
| `/quick/write` | POST | Sync single writes |
| `/quick/read` | POST | Sync single reads |
| `/quick/bulk-write` | POST | Sync batch writes |
| `/quick/bulk-read` | POST | Sync batch reads |
| `/quick/concurrent` | POST | Sync concurrent test |
| `/run` | POST | Custom benchmark (JSON body) |
| `/run/full` | POST | Full benchmark suite |
| `/cleanup` | DELETE | Clear all test data |
| `/presets` | GET | Get preset configurations |

---

### 🎛️ Custom Benchmark

Full control over all parameters:

```bash
curl -X POST "http://localhost:8080/api/benchmark/run" \
  -H "Content-Type: application/json" \
  -d '{
    "operationType": "BULK_WRITE",
    "recordCount": 50000,
    "batchSize": 500,
    "threadCount": 8,
    "warmupEnabled": true,
    "warmupIterations": 100,
    "cleanupAfter": true,
    "databases": ["MYSQL", "REDIS"]
  }'
```

#### Request Parameters:

| Parameter | Type | Default | Range | Description |
|-----------|------|---------|-------|-------------|
| `operationType` | enum | - | Required | WRITE, READ, BULK_WRITE, BULK_READ, CONCURRENT_READ_WRITE |
| `recordCount` | int | 1000 | 1 - 1,000,000 | Number of records |
| `batchSize` | int | 100 | 1 - 10,000 | Records per batch |
| `threadCount` | int | 4 | 1 - 64 | Concurrent threads |
| `warmupEnabled` | bool | true | - | Run warmup first |
| `warmupIterations` | int | 100 | 0 - 1000 | Warmup operations |
| `cleanupAfter` | bool | true | - | Delete data after |
| `databases` | array | all | - | Filter databases |

---

### 📊 Response Format

```json
{
  "benchmarkId": "abc123-uuid",
  "operationType": "Bulk Write",
  "recordCount": 10000,
  "totalDurationMs": 5234,
  "success": true,
  
  "results": [
    {
      "database": "Redis",
      "recordCount": 10000,
      "successCount": 10000,
      "successRate": 100.0,
      "totalTimeMs": 1250,
      "averageTimeMs": 0.125,
      "minTimeMs": 0,
      "maxTimeMs": 15,
      "p50TimeMs": 0.1,
      "p95TimeMs": 0.35,
      "p99TimeMs": 0.8,
      "operationsPerSecond": 8000.0
    }
  ],
  
  "comparison": {
    "fastestDatabase": "Redis",
    "slowestDatabase": "MySQL",
    "relativePerformance": {
      "Redis": 1.0,
      "Aerospike": 2.0,
      "MySQL": 12.0
    }
  }
}
```

---

### 📈 Console Output

When running async benchmarks, results appear in the console:

```
╔══════════════════════════════════════════════════════════════════════════════╗
║  BENCHMARK RESULTS: BULK_WRITE                                               ║
╠══════════════════════════════════════════════════════════════════════════════╣
║  Records: 10000        | Threads: 4        | Total Time: 2.34 s              ║
╠══════════════════════════════════════════════════════════════════════════════╣
║  Database    │ Avg (ms)  │ Min (ms)  │ Max (ms)  │ P95 (ms)  │ Ops/sec     ║
╠══════════════════════════════════════════════════════════════════════════════╣
║  Redis      │     0.125 │         0 │        15 │     0.350 │     8000.0 🏆║
║  Aerospike  │     0.250 │         0 │        22 │     0.600 │     4000.0  ║
║  MySQL      │     1.500 │         1 │        45 │     4.200 │      666.7 🐢║
╠══════════════════════════════════════════════════════════════════════════════╣
║  🏆 Winner: Redis        (12.00x faster than MySQL)                          ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

---

## 📝 Test Data

Each benchmark record simulates a realistic user/employee profile:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "firstName": "John",
  "lastName": "Smith",
  "email": "john.smith@example.com",
  "phoneNumber": "+1-555-123-4567",
  "address": "123 Main Street",
  "city": "San Francisco",
  "country": "United States",
  "company": "Acme Corporation",
  "jobTitle": "Senior Software Engineer",
  "department": "Engineering",
  "salary": 125000.50,
  "age": 34,
  "isActive": true,
  "createdAt": "2026-01-06T08:30:00.000Z",
  "updatedAt": "2026-01-06T08:30:00.000Z",
  "metadata": "{\"source\":\"benchmark\",\"version\":\"1.0.0\"}"
}
```

**Record Size:** ~500-700 bytes  
**Generated Using:** [Datafaker](https://www.datafaker.net/) library

---

## 🎯 Use Cases

### 1. Database Selection
> "Which database should I use for my caching layer?"

```bash
curl -X POST "http://localhost:8080/api/benchmark/async/bulk-write?count=100000"
```

### 2. Capacity Planning
> "How many operations per second can each database handle?"

```bash
curl -X POST "http://localhost:8080/api/benchmark/async/concurrent?count=50000&threads=16"
```

### 3. Latency Requirements
> "I need sub-millisecond reads. Which database can deliver?"

```bash
curl -X POST "http://localhost:8080/api/benchmark/async/read?count=10000"
```

### 4. Batch Processing
> "I need to bulk insert millions of records nightly. What's the best approach?"

```bash
curl -X POST "http://localhost:8080/api/benchmark/async/bulk-write?count=1000000&batchSize=1000"
```

### 5. Mixed Workload
> "My app does 70% reads, 30% writes with 8 concurrent users."

```bash
curl -X POST "http://localhost:8080/api/benchmark/async/concurrent?count=10000&threads=8"
```

---

## ⚙️ Configuration

### Application Properties

Edit `db-metrics-api/src/main/resources/application.yaml`:

```yaml
# Server
server:
  port: 8080

# MySQL
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/benchmark
    username: repl
    password: ci

# Redis  
  data:
    redis:
      host: localhost
      port: 6379

# Aerospike
dbmetrics:
  aerospike:
    host: localhost
    port: 3000
    namespace: benchmark
    set: records
```

### Docker Compose

The `docker-compose.yaml` configures all three databases:

- **MySQL 8.0** - Port 3306
- **Redis** (Bitnami) - Port 6379  
- **Aerospike** - Port 3000

---

## 📁 Project Structure

```
DB-Metrics-Checker/
├── db-metrics-common/          # Shared code
│   └── src/main/java/
│       └── com/dbmetrics/common/
│           ├── model/          # BenchmarkRecord, BenchmarkMetrics, etc.
│           ├── operations/     # DatabaseOperations interface
│           ├── generator/      # MockDataGenerator
│           └── metrics/        # MetricsCollector
│
├── db-metrics-mysql/           # MySQL implementation
│   └── src/main/java/
│       └── com/dbmetrics/mysql/
│           ├── entity/         # JPA Entity
│           ├── repository/     # Spring Data Repository
│           └── service/        # MySqlDatabaseOperations
│
├── db-metrics-redis/           # Redis implementation
│   └── src/main/java/
│       └── com/dbmetrics/redis/
│           ├── config/         # RedisTemplate config
│           └── service/        # RedisDatabaseOperations
│
├── db-metrics-aerospike/       # Aerospike implementation
│   └── src/main/java/
│       └── com/dbmetrics/aerospike/
│           ├── config/         # Aerospike client config
│           └── service/        # AerospikeDatabaseOperations
│
├── db-metrics-orchestrator/    # Benchmark coordination
│   └── src/main/java/
│       └── com/dbmetrics/orchestrator/
│           ├── runner/         # BenchmarkRunner
│           └── service/        # BenchmarkOrchestrator
│
├── db-metrics-api/             # REST API
│   └── src/main/java/
│       └── com/dbmetrics/api/
│           ├── controller/     # BenchmarkController
│           ├── dto/            # Request/Response DTOs
│           └── config/         # OpenAPI config
│
├── docker-compose.yaml         # Database containers
├── pom.xml                     # Parent POM
└── README.md
```

---

## 🛠️ Development

### Build
```bash
./mvnw clean install
```

### Run Tests
```bash
./mvnw test
```

### Run with Debug
```bash
./mvnw spring-boot:run -pl db-metrics-api -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

---

## 📄 License

MIT License - feel free to use this for your projects!

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

---

Made with ❤️ for database performance enthusiasts
