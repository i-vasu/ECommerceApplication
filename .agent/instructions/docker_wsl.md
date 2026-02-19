# Docker in WSL Instructions

This project uses Docker for infrastructure (PostgreSQL, Dragonfly/Redis, etc.). **Docker is installed inside the WSL 2 environment.**

### 🚀 Running Docker Commands
When interacting with Docker, you MUST prefix your commands with `wsl`.

| Task | Command |
|------|---------|
| List Networks | `wsl docker network ls` |
| Create Network | `wsl docker network create ecommerce-net` |
| Start Infrastructure | `wsl docker compose up -d` |
| View All Logs | `wsl docker compose logs -f` |
| Stop Services | `wsl docker compose down` |
| Backend Logs | `wsl docker logs modulith-service -f` |

### 🛠️ Prerequisites
Before running `docker compose up`, ensure the `ecommerce-net` network exists:
```bash
wsl docker network create ecommerce-net
```

### 🌍 Accessing Services
Even though Docker runs in WSL, WSL 2 maps its ports to `localhost` on Windows.
- **Java Backend:** `http://localhost:8080`
- **PostgreSQL:** `localhost:5432`
- **Redis (Dragonfly):** `localhost:6379`
- **Umami Analytics:** `http://localhost:3300`

### 5. Tech Stack Compliance
- **Rule**: Ensure all Java code is compatible with Java 25.
- **Rule**: Ensure all Spring Boot configurations are compatible with Spring Boot 4.0.2.
- **Rule**: Never downgrade the Java or Spring Boot versions.
