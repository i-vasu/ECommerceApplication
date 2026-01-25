## 2026-01-21 - Memory Bloat in Entity Checks
**Learning:** Fetching a whole collection (`category.getProducts()`) just to check if an item exists is a common N+1/O(N) trap, especially with Hibernate proxies.
**Action:** Always prefer `existsBy...` repository methods for existence checks. It leverages the DB index and returns a single boolean.
