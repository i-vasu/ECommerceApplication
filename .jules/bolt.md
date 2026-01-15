## 2026-01-15 - Testing with H2 in Sandbox
**Learning:** Sandbox environment lacks running Postgres. To run JPA tests, must inject H2 dependency and config.
**Action:** Use temporary H2 setup for verification, then revert before submit if dependency changes aren't authorized.
