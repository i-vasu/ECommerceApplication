# Changelog

All notable changes to the **Java Middleware** will be documented in this file.

## [2026-01-09] - PoC Integration & Security hardening
### Branch: `develop`
- **Infrastructure**: Set up Docker-based ERPNext v15 with official production-ready configuration.
- **Logic**: Fixed `specialPrice` calculation in `ERPNextService` to match frontend expectations.
- **Imaging**: Implemented `GET /api/public/products/image/{name}` endpoint to serve ERPNext product images through the middleware.
- **Security**: Moved all sensitive keys (ERPNext API Keys, DB Passwords, JWT Secrets) from `application.properties` to Environment Variables.
- **DevOps**: Added GitHub Actions CI/CD pipeline (`java-ci.yml`).
- **Context**: Added `.agent/workflows/` with `branching_strategy.md` and `agent_instructions.md`.
- **Roadmap**: Rewrote `TODO.md` with 3-phase production plan.
