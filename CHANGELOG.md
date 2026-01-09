# Changelog

All notable changes to the **Java Middleware** will be documented in this file.

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
