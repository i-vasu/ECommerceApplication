## 2026-02-05 - N+1 Query Mitigation
**Learning:** Verified that `@BatchSize` annotation works for solving N+1 query problems in Hibernate for `@OneToMany` collections.
**Action:** Always verify existence of fields before applying annotations. In this project, ensure Java 25 compatibility for local builds or rely on CI.
