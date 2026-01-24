## 2026-01-24 - Java Version Mismatch Strategy
**Learning:** The project is configured for Java 25 (preview features) but the environment runs Java 21. This prevents `mvn test` and even `mvn compile` from succeeding for all modules.
**Action:** When working in this environment, prioritize static analysis and manual verification of code changes over reliance on the full test suite. Avoid attempting to downgrade the project Java version unless explicitly instructed.
