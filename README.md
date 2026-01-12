# Fashion E-Commerce Middleware (Java)

This repository contains the Java Spring Boot middleware for a headless fashion e-commerce platform. It acts as an API Gateway and business logic adapter between **ERPNext (Backend)** and **Next.js (Storefront)**.

## 🚀 Quick Start (Dockerized)

### 1. Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- `.env` file populated with ERPNext API keys and MinIO credentials (use `.env.example` as a template)

### 2. Launch Observability & Caching
First, start the monitoring stack (BanyanDB, SkyWalking, Redis). This stack is optimized for high ingestion (100 RPS) with shared memory savings.
```powershell
docker compose -f docker-compose.o11y.yml up -d
```
- **SkyWalking UI**: [http://localhost:8081](http://localhost:8081)
- **Redis**: Running on port `6380` (Persistent)

### 3. Launch Application, Database & Storage
This will build the Java jar with the SkyWalking Agent attached, start the Postgres database, and initialize the MinIO object store.
```powershell
docker compose up -d --build
```
- **Java API**: [http://localhost:8080](http://localhost:8080)
- **MinIO Console**: [http://localhost:9001](http://localhost:9001) (User: `minioadmin`, Pass: `minioadmin`)
- **Swagger Docs**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## 📂 Persistence & Security
- **Data Location**: All persistent data is stored in the local `./data/` directory:
  - `./data/postgres`: Application database records.
  - `./data/minio`: High-res photos and videos.
  - `./data/banyandb`: Tracing and metrics data.
  - `./data/redis`: Cache data (Append-only mode).
- **Volumes**: Using bind mounts for direct visibility and easy backups. Data persists even if containers are destroyed.

---

## 🛠️ Tech Stack & Scaling
- **JDK**: 21 (Temurin)
- **Storage**: PostgreSQL 15 + MinIO (Object Storage)
- **Observability**: Apache SkyWalking 9.5.0 with BanyanDB storage.
- **Scaling Analysis**: 
  - **100 RPS Ready**: BanyanDB is utilized to save ~8GB of RAM compared to Elasticsearch.
  - **Binary Offloading**: All heavy media is served via MinIO to keep the database and ERP lean.

---

## 🧪 Testing
To run local unit and integration tests:
```powershell
mvn test
```
*Note: The project uses a custom Maven Surefire configuration to handle Asia/Kolkata timezone mismatches automatically.*

## 🎨 Storefront Integration
Ensure the Next.js storefront is pointing to the Java backend via `NEXT_PUBLIC_JAVA_BACKEND_URL=http://localhost:8080/api`.
