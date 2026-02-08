## 2026-02-08 - [Optimized Product Existence Check]
**Learning:** Checking for entity existence by iterating over a fetched collection (e.g., `category.getProducts()`) causes N+1 problems and loads unnecessary data into memory. This is especially bad when the collection is large.
**Action:** Always prefer `existsBy...` repository methods (e.g., `existsByCategoryAndProductNameAndDescription`) which execute an optimized `SELECT 1` or `COUNT(*)` query in the database.
