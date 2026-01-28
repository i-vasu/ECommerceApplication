## 2026-01-28 - Java Version Mismatch in CI/CD vs Project
**Learning:** The project is configured for Java 25 (`pom.xml`), but the execution environment runs Java 21. This mismatch prevents running `mvn compile` or `mvn test` locally without modifying the build configuration (which is restricted).
**Action:** In this environment, prioritize static analysis, visual code verification, and "safe" pattern-based optimizations (like standard Hibernate annotations) over reliance on local test execution. Document this constraint clearly.
