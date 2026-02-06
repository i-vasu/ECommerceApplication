## 2026-02-06 - Java Version Mismatch
**Learning:** Local environment runs Java 21, but project (`modulith-kernel`) requires Java 25 features (preview). Local build/test is impossible.
**Action:** Rely on static analysis locally. Trust CI for verification (which uses Java 25).

## 2026-02-06 - N+1 Optimization Pattern
**Learning:** Entities with multiple `@OneToMany` collections (`Product`) are prone to N+1 problems.
**Action:** Apply `@BatchSize(size = 20)` to these collections to enable batch fetching (N queries -> N/20 queries).
