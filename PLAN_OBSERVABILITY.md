# Tech Stack Onboarding Plan: High-Load Observability & Performance

## Objective
Onboard Redis (Caching), Apache SkyWalking with **BanyanDB** (Observability), MinIO (Object Storage), and Framer Motion (Premium UI) to support a high-performance fashion store architecture capable of 100 RPS.

---

## 1. Observability: Apache SkyWalking + BanyanDB
Based on your requirement for efficiency, we have opted for **BanyanDB** over Elasticsearch.
- **Why BanyanDB?**: 
  - **Memory Efficiency**: Nearly **5x less RAM** usage than Elasticsearch.
  - **Write Throughput**: 24% higher than Elasticsearch at 100 RPS.
  - **Purpose-Built**: Specifically designed for SkyWalking's data model (Metrics/Traces/Logs).
- **RAM Analysis (100 RPS Baseline)**:
  - SkyWalking OAP: ~2GB (Ingestion brain)
  - **BanyanDB (Storage)**: ~1GB (Highly efficient time-series store)
  - SkyWalking UI: ~256MB
  - **Total RAM Needed for O11y**: ~3.5GB - 4GB (Massive saving vs. 10GB for ES stack).

## 2. Object Storage: MinIO
- **Usage**: High-performance storage for high-res product photos and 4K fashion videos.
- **Benefit**: Offloads heavy binary data from ERPNext and the main Postgres DB. S3-compatible API for future-proof scaling to AWS.
- **Action**: 
    - Dedicated MinIO service on **port 9000** (API) and **9001** (Console).
    - Integrated `io.minio` Java Client in `pom.xml`.
    - Automated bucket creation via `MinioService.java`.

## 3. Performance: Redis
- **Usage**: Product catalog caching and session management to achieve sub-100ms response times.
- **Action**: 
  - Dedicated Redis service on **port 6380**.
  - Integrated `spring-boot-starter-data-redis` in `pom.xml`.

## 4. UI/UX: Framer Motion
- **Usage**: Elevating the "Premium" brand feel with smooth transitions.
- **Action**: 
  - Installed `framer-motion` in `vaabhi-storefront`.
  - Initialized `components/premium-motion.tsx`.

---

## Load Analysis (100 RPS @ Peak with BanyanDB & MinIO)
- **Java Middleware**: ~2GB RAM.
- **DB (Postgres)**: ~1GB RAM.
- **MinIO**: ~512MB RAM.
- **Redis**: ~512MB RAM.
- **ERPNext Core**: ~2GB - 4GB RAM.
- **Observability (BanyanDB Stack)**: ~4GB RAM.
- **Grand Total (Peak Projection)**: **~11GB - 13GB RAM**. 
  *(Still significantly lower than the 18GB+ required for an Elasticsearch stack).*

---

## Execution
Run the following to start the optimized stack:
```powershell
# Start Observability Stack
docker compose -f docker-compose.o11y.yml up -d

# Start Core Application & Storage
docker compose up -d --build
```
