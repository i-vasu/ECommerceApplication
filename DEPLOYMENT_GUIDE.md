# Deployment Guide: Observability Stack + Admin Optimization

## What's Been Completed

### ✅ Phase 1: Observability Stack Setup
- Created `docker-compose.observability.yml`
- Created Prometheus, Grafana, Zipkin, Loki configurations
- Updated `product-service/pom.xml` (removed MinIO, added observability)
- Updated `order-service/pom.xml` (removed SkyWalking & MinIO, added observability)
- Updated `order-service/application.properties`
- Updated `order-service/logback-spring.xml` (added Loki)

### ✅ Phase 2: Admin Tools Optimization
- Reduced Metabase: 1GB → 512MB
- Reduced Dittofeed: 512MB → 256MB
- Reduced Umami: 256MB → 128MB

### ⏳ Pending
- Update `marketplace-service/pom.xml`
- Update `product-service/application.properties`
- Update `product-service/logback-spring.xml`
- Remove SkyWalking from `docker-compose.yml`
- Remove MinIO from `docker-compose.yml`

---

## Quick Start

### Step 1: Build Services

```powershell
cd C:\Users\alway\OneDrive\Desktop\Vasu\ECommerceApplication

# Build all services
mvn clean package -DskipTests
```

### Step 2: Start Observability Stack

```powershell
# Start observability services
docker-compose -f docker-compose.observability.yml up -d

# Verify services
docker-compose -f docker-compose.observability.yml ps
```

### Step 3: Verify Observability Stack

Open in browser:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Zipkin: http://localhost:9411
- Loki: http://localhost:3100/ready

### Step 4: Restart Application Services

```powershell
# Restart services to pick up new dependencies
docker-compose restart order-service product-service marketplace-service
```

### Step 5: Verify Metrics

```powershell
# Check if services are exposing metrics
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8082/actuator/prometheus
curl http://localhost:8083/actuator/prometheus
```

---

## RAM Savings Summary

### Before
```
SkyWalking:     5.3GB
MinIO:          1GB
Metabase:       1GB
Dittofeed:      512MB
Umami:          256MB
───────────────────
Total:          8.1GB
```

### After
```
Prometheus:     256MB
Grafana:        256MB
Zipkin:         400MB
Loki:           300MB
Promtail:       128MB
Metabase:       512MB
Dittofeed:      256MB
Umami:          128MB
───────────────────
Total:          2.2GB
```

**Savings: 5.9GB (73% reduction!)** ✅

---

## Next Steps

1. Complete remaining service updates (marketplace-service, product-service)
2. Remove SkyWalking from docker-compose.yml
3. Remove MinIO from docker-compose.yml
4. Test all functionality
5. Import Grafana dashboards
6. Set up alerts

---

## Grafana Dashboards

### Import Pre-built Dashboards

1. Login to Grafana (http://localhost:3000)
2. Go to Dashboards → Import
3. Import these dashboard IDs:
   - **4701** - JVM Micrometer
   - **11378** - Spring Boot 2.1 Statistics
   - **12900** - Spring Boot Statistics

---

## Troubleshooting

### Services not showing in Prometheus

```powershell
# Check if actuator endpoints are exposed
curl http://localhost:8081/actuator

# Check Prometheus targets
# Open http://localhost:9090/targets
```

### Logs not appearing in Loki

```powershell
# Check Loki health
curl http://localhost:3100/ready

# Check Promtail logs
docker logs promtail
```

### Zipkin not showing traces

```powershell
# Verify Zipkin endpoint in application.properties
# management.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans

# Check Zipkin UI
# Open http://localhost:9411
```

---

## Rollback Plan

If issues occur:

```powershell
# Stop observability stack
docker-compose -f docker-compose.observability.yml down

# Revert code changes
git checkout -- order-service/pom.xml
git checkout -- product-service/pom.xml
git checkout -- order-service/src/main/resources/

# Restart with SkyWalking
docker-compose -f docker-compose.o11y.yml up -d
```
