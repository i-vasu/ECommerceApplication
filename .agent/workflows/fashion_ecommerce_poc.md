---
description: Fashion e‑Commerce PoC – ERPNext, Java middleware, Next.js storefront
---

# Overview
This workflow walks you through setting up a **Proof‑of‑Concept** fashion e‑commerce platform consisting of:
- **ERPNext** (Docker Desktop) – handles inventory, orders, accounting.
- **Java Spring Boot middleware** – custom business logic, exposes REST APIs.
- **Next.js storefront** – modern React‑based UI for customers.

The steps assume you have the two active workspaces open:
- `c:/Users/alway/OneDrive/Desktop/Vasu/ECommerceApplication` (Java backend)
- `c:/Users/alway/OneDrive/Desktop/Vasu/vaabhi-storefront` (Next.js UI)

---

## 1️⃣ Prepare ERPNext (Docker Desktop)
```bash
# 1.1 Pull the official ERPNext Docker image (uses MariaDB & Redis)
// turbo
docker pull frappe/erpnext:version-15

# 1.2 Create a docker‑compose.yml for ERPNext (place in a folder named `erpnext` at the repo root)
mkdir erpnext && cd erpnext
cat > docker-compose.yml <<'EOF'
version: "3.8"
services:
  redis-cache:
    image: redis:6-alpine
    restart: unless-stopped
  redis-queue:
    image: redis:6-alpine
    restart: unless-stopped
  mariadb:
    image: mariadb:10.6
    environment:
      MYSQL_ROOT_PASSWORD: example_root_pw
      MYSQL_DATABASE: erpnext
      MYSQL_USER: erpnext
      MYSQL_PASSWORD: example_pw
    volumes:
      - mariadb-data:/var/lib/mysql
    restart: unless-stopped
  site-creator:
    image: frappe/erpnext-worker:version-15
    command: new
    environment:
      SITE_NAME: demo.local
      DB_ROOT_USER: root
      MYSQL_ROOT_PASSWORD: example_root_pw
      ADMIN_PASSWORD: admin
      INSTALL_APPS: erpnext
    depends_on:
      - mariadb
      - redis-cache
      - redis-queue
    restart: "no"
  erpnext:
    image: frappe/erpnext-worker:version-15
    environment:
      SITE_NAME: demo.local
      DB_ROOT_USER: root
      MYSQL_ROOT_PASSWORD: example_root_pw
      ADMIN_PASSWORD: admin
    depends_on:
      - mariadb
      - redis-cache
      - redis-queue
    ports:
      - "8000:8000"
    restart: unless-stopped
volumes:
  mariadb-data:
EOF
```

```bash
# 1.3 Start ERPNext
// turbo
docker compose up -d
```

> **Tip:** Open `http://localhost:8000` → login with *admin / admin*.
---

## 2️⃣ Build & Run Java Middleware
```bash
# 2.1 Ensure you have JDK 21+ and Maven installed.
java -version
mvn -v
```

```bash
# 2.2 From the Java project root, package the app as a Docker image.
cd c:/Users/alway/OneDrive/Desktop/Vasu/ECommerceApplication
cat > Dockerfile <<'EOF'
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
EOF
```

```bash
# 2.3 Build the Docker image (name: ecommerce-middleware)
// turbo
docker build -t ecommerce-middleware .
```

```bash
# 2.4 Run the container, linking it to ERPNext network (use same Docker network as ERPNext)
// turbo
docker run -d --name middleware --network $(docker compose -f erpnext/docker-compose.yml ps -q | head -n1) -p 8080:8080 ecommerce-middleware
```
---

## 3️⃣ Configure Next.js Storefront
```bash
# 3.1 Install dependencies (if not already done)
cd c:/Users/alway/OneDrive/Desktop/Vasu/vaabhi-storefront
npm install
```

```bash
# 3.2 Add environment variables for API endpoints (create .env.local)
cat > .env.local <<'EOF'
NEXT_PUBLIC_API_BASE=http://localhost:8080/api
NEXT_PUBLIC_ERP_URL=http://localhost:8000
EOF
```

```bash
# 3.3 Update `package.json` scripts to run with Docker (optional) – we’ll keep the dev script.
# No code change needed for PoC; just run the dev server.
```

```bash
# 3.4 Start the Next.js dev server
// turbo
npm run dev
```

Open `http://localhost:3000` – you should see the storefront UI. Hook up UI components to the Java API (e.g., `/products`, `/orders`).
---

## 4️⃣ Verify End‑to‑End Flow
1. **Create a product** in ERPNext UI → *Items* → *Add New*.
2. **Expose product data** via a new REST endpoint in the Java middleware (e.g., `GET /api/products`).
3. **Consume the endpoint** in a Next.js page (`pages/products.tsx`).
4. **Place an order** through the storefront → Java forwards to ERPNext using its REST API (`/api/resource/Sales%20Order`).
---

## 5️⃣ Clean‑up
```bash
# Stop all containers
docker compose -f erpnext/docker-compose.yml down
docker stop middleware && docker rm middleware
```
---

# 🎉 Done!
Follow the steps in order. If any command fails, check Docker Desktop logs or the Java build output. Feel free to ask for more detailed code snippets (e.g., a sample Spring controller or a Next.js product list component).
