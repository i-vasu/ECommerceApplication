## 2026-01-31 - Environment Mismatch Blocked Local Verification
**Learning:** The project requires Java 25 (preview features like `import module`, `ScopedValue` in `modulith-kernel`) but the local environment runs Java 21. This prevents local compilation and testing of dependent modules like `modulith-product`.
**Action:** Rely on static analysis for Java 25 specific code. Trust standard library/framework patterns (like Hibernate annotations) when local verification is impossible due to environment constraints.

## 2026-01-31 - N+1 Mitigation via BatchSize
**Learning:** `Product` entity has multiple `@OneToMany` lazy collections (`variants`, `media`, `reviews`, etc.) that are accessed during DTO mapping (e.g. in `ProductMapper`), causing N+1 queries during list operations (`getAllProducts`).
**Action:** Applied `@BatchSize(size = 20)` to these collections. This is a standard Hibernate optimization that transparently batches fetches (using `IN` clause), reducing queries from 1+N*M to 1+(N/BatchSize)*M.

## 2026-01-31 - CI Workflow Failures & Fixes
**Learning:** CI failed due to multiple misconfigurations:
1. `docker.io/dragonflydb/dragonfly` image manifest was missing/invalid for the runner platform. Switched to `redis:alpine`.
2. `ci-cd.yml` referenced non-existent modules (`order-service`, `product-service`) and used deprecated `dependency-check:check`. Updated to correct module names (`modulith-order`, `modulith-product`) and plugin prefix (`org.owasp:dependency-check-maven`).
3. Playwright installation failed with `ClassNotFoundException` because `mvn exec:java` was run from root without specifying the module (`-f automation-tests/pom.xml`) containing the dependencies.
4. MinIO service in GitHub Actions `java-ci.yml` failed to start because the `minio/minio` image requires a command argument (`server /data`), which the `services` block does not easily support passing.
**Action:** Always verify module names and plugin configurations match `pom.xml`. For complex service containers requiring arguments, run them as a `docker run` step instead of a workflow service.
