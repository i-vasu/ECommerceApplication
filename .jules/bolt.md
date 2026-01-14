## 2026-01-14 - [Inefficient Duplicate Check]
**Learning:** Found an O(N) loop iterating over a lazy-loaded collection in ProductServiceImpl.addProduct to check for duplicates. This triggers fetching all products of a category into memory.
**Action:** Always prefer database queries (existsBy... or findBy...) for existence checks instead of loading collections.
