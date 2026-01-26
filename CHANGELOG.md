# Changelog

All notable changes to the **Java Middleware** will be documented in this file.

## [2026-01-17] - Architecture Modularity & Memory Optimization
### Branch: `develop`
- **Architecture**: Implemented **Spring Modulith** across `product-service`, `order-service`, and `marketplace-service` for better modular boundary enforcement.
- **Documentation**: Integrated **Swagger (OpenAPI)** for all services, accessible via `/swagger-ui.html`.
- **Optimization**: Implemented **Memory Leak Prevention Strategy**.
    - Refactored `ProductDataFlowService` to use **streaming** for image processing, eliminating large heap allocations.
    - Updated `ImageService` to ensure safe resource management with `try-with-resources`.
    - Documented strategy in `.agent/workflows/memory_leak_prevention_strategy.md`.
- **Refactoring**:
    - Updated `Lombok` to `edge-SNAPSHOT` to resolve Java 25 compatibility issues.
    - Cleaned up duplicate dependencies in all `pom.xml` files.
    - Fixed compilation issues related to variable shadowing and missing imports.
- **Dependencies**: Added `spring-session-data-redis` to `marketplace-service` for distributed session handling.

## [2026-01-12] - Scaling & Observability Onboarding
### Branch: `develop`

- **Performance**: Integrated Redis for Java Middleware on port 6380 via `spring-boot-starter-data-redis`.
- **UI/UX**: Onboarded `framer-motion` in the Storefront and created `PremiumHover` and `FadeIn` motion components.
- **Analysis**: Created `PLAN_OBSERVABILITY.md` with detailed RAM analysis for 100 RPS peak load.

## [2026-01-09] - PoC Integration, Security, and Testing Hardening
### Branch: `develop`
- **Infrastructure**: Set up Docker-based ERPNext v15 with official production-ready configuration.
- **Logic**: Fixed `specialPrice` calculation in `ERPNextService` to match frontend expectations.
- **Imaging**: Implemented `GET /api/public/products/image/{name}` endpoint to serve ERPNext product images through the middleware.
- **Security**: Moved all sensitive keys (ERPNext API Keys, DB Passwords, JWT Secrets) to Environment Variables and protected them with `.gitignore`.
- **Testing**: 
    - Resolved PostgreSQL Timezone mismatch (`Asia/Calcutta` vs `Asia/Kolkata`) by forcing `user.timezone` in Maven Surefire.
    - Specialized `ERPNextService` to use Dependency Injection for `RestTemplate`, enabling clean mocking.
    - Added comprehensive integration test for ERPNext API handshake.
- **DevOps**: Enhanced GitHub Actions CI/CD to spin up dynamic PostgreSQL containers for automated test verification on every push.
- **Governance**: Implemented "Critical Pillars" in `agent_instructions.md` covering Design, Architecture Unity, and Quality Gates.
- **Roadmap**: Rewrote `TODO.md` with 3-phase production plan.
