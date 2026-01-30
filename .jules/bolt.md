## 2026-01-30 - Environment Mismatch Blocking Builds
**Learning:** The project is configured for Java 25 (`<source>25</source>` in `pom.xml`). CI builds failed on Java 21 runners.
**Action:** Configure CI workflows to use `java-version: '25-ea'` and `distribution: 'zulu'` when building Java 25 projects.

## 2026-01-30 - Optimization Pattern: BatchSize
**Learning:** `OneToMany` collections in `Product` and `Category` entities were missing `@BatchSize`, leading to potential N+1 query issues.
**Action:** Always check `OneToMany` relationships for `@BatchSize` or `FETCH JOIN` usage during performance reviews.

## 2026-01-30 - CI Configuration Failures
**Learning:**
1. Maven module names in CI (`ci-cd.yml`) must match exactly (`modulith-order` vs `order-service`).
2. MinIO containers require an explicit startup command in GitHub Actions (`server /data`).
3. Dependency Check plugin requires full coordinates (`mvn org.owasp:dependency-check-maven:check`) if not in default groups.
**Action:** Verify Maven module names against `pom.xml` and consult container documentation for startup commands when updating CI scripts.
