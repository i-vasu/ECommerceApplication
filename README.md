# Vasu E-Commerce Modular Monolith

This project is a high-performance fashion e-commerce modular monolith built with the latest 2026 Java and Spring Boot stack.

## 🚀 Modern Tech Stack
- **Java 25 (LTM)**: Utilizing Scoped Values, Virtual Threads, Sequenced Collections, and Vector API.
- **Spring Boot 4.0**: Optimized for modern observability and high-concurrency startups.
- **Database**: PostgreSQL with Multi-tenant schema isolation.
- **Monitoring**: OpenTelemetry + Prometheus/Grafana integration.

## 🛠️ Build & Run

### Prerequisites
- JDK 25
- Maven 3.9+
- Docker (for PostgreSQL & Redis)

### Standard Build
```bash
mvn clean install -pl modulith-service -DskipTests
```

### Optimized Startup (< 1s)

#### 1. Spring Boot CDS (Class Data Sharing)
To reduce class-loading time, generate a shared archive:
```bash
./modulith-service/scripts/cds_train.sh
```
Then run with the `optimized` profile:
```bash
mvn spring-boot:run -pl modulith-service -Poptimized -Dspring-boot.run.profiles=perf
```

#### 2. Project CRaC (Checkpoint/Restore)
For near-instant startup by restoring a pre-initialized JVM state:
```bash
# Snapshot (Checkpoint)
./modulith-service/scripts/crac_checkpoint.sh

# Instant Restore
./modulith-service/scripts/crac_restore.sh
```

## 📐 Architecture
The project follows **Spring Modulith** principles. Each top-level package in `com.app` represents a logically separated domain module.

## 🧪 Testing
```bash
mvn test -pl modulith-service
```
Integration tests use **RestTestClient** and **Testcontainers** for isolated environment validation.
