## 2026-01-21 - Environment Java Version Mismatch
**Learning:** The project is configured for Java 25 (preview features enabled), but the local environment runs Java 21. This mismatch prevents compiling and running the full test suite locally.
**Action:** When working on this project in this environment, rely on static analysis, careful code review, and adherence to standard patterns. Avoid attempting to run `mvn test` if it requires Java 25 features.
