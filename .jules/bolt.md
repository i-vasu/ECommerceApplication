## 2026-01-16 - Integration Tests with H2
**Learning:** The default tests fail because they try to connect to a PostgreSQL database on localhost:5432, which is not available in the CI/Sandbox environment. The solution is to configure tests to use an in-memory database like H2.
**Action:** Always check `src/test/resources/application.properties` or similar to ensure tests are configured to run in isolation.
