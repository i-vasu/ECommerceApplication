## 2026-01-30 - Environment Mismatch Blocking Builds
**Learning:** The project is configured for Java 25 (`<source>25</source>` in `pom.xml`) but the execution environment runs Java 21. This causes `mvn install` to fail with `release version 25 not supported`. Modifying `pom.xml` is prohibited.
**Action:** When verification via build/test is impossible due to this mismatch, rely on rigorous static analysis and manual code verification, explicitly documenting the limitation.

## 2026-01-30 - Optimization Pattern: BatchSize
**Learning:** `OneToMany` collections in `Product` and `Category` entities were missing `@BatchSize`, leading to potential N+1 query issues.
**Action:** Always check `OneToMany` relationships for `@BatchSize` or `FETCH JOIN` usage during performance reviews.

## 2026-01-30 - CI Configuration Failures
**Learning:** CI workflows (`ci-cd.yml`) used incorrect module names (`order-service` instead of `modulith-order`) and missing dependency check plugins, causing build failures.
**Action:** Verify Maven module names against `pom.xml` `<modules>` section when updating CI scripts. Ensure used plugins are defined in `pom.xml`.
