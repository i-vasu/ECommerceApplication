## 2026-01-31 - Environment Mismatch Blocked Local Verification
**Learning:** The project requires Java 25 (preview features like `import module`, `ScopedValue` in `modulith-kernel`) but the local environment runs Java 21. This prevents local compilation and testing of dependent modules like `modulith-product`.
**Action:** Rely on static analysis for Java 25 specific code. Trust standard library/framework patterns (like Hibernate annotations) when local verification is impossible due to environment constraints.

## 2026-01-31 - N+1 Mitigation via BatchSize
**Learning:** `Product` entity has multiple `@OneToMany` lazy collections (`variants`, `media`, `reviews`, etc.) that are accessed during DTO mapping (e.g. in `ProductMapper`), causing N+1 queries during list operations (`getAllProducts`).
**Action:** Applied `@BatchSize(size = 20)` to these collections. This is a standard Hibernate optimization that transparently batches fetches (using `IN` clause), reducing queries from 1+N*M to 1+(N/BatchSize)*M.
