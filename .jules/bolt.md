## 2026-02-11 - Java Version Mismatch in Local Environment
**Learning:** The project `modulith-kernel` requires Java 25 features (e.g., `ScopedValue`), but the local environment runs Java 21. This prevents local compilation and testing of dependent modules.
**Action:** Rely on static analysis and manual verification for local changes. Ensure CI configuration handles the Java 25 requirement (e.g., `java-version: '25-ea'`).

## 2026-02-11 - N+1 Query Optimization
**Learning:** Hibernate's `@BatchSize` annotation is an effective way to solve N+1 query problems for `@OneToMany` collections without changing the fetching strategy to `EAGER` or rewriting queries.
**Action:** Apply `@BatchSize(size = 20)` to lazy-loaded collections in entities that are frequently accessed in lists (e.g., `Product.variants`, `Order.orderItems`).
