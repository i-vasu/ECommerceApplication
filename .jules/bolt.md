## 2026-02-09 - N+1 Query Optimization
**Learning:** Spring Data JPA's default lazy loading can lead to severe N+1 query problems when accessing child collections in loops (e.g., listing products with variants).
**Action:** Apply `@BatchSize(size = 20)` to `@OneToMany` collections. This reduces N+1 queries to (N/BatchSize)+1, significantly improving performance for list views.
