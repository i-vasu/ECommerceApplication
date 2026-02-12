## 2026-02-12 - N+1 Problem in Product and Category Entities
**Learning:**
- **Performance Bottleneck:** The `Product` entity has multiple `@OneToMany` collections (`variants`, `media`, `reviews`, `bundleItems`, `priceLists`) and `Category` has `products` collection. Fetching lists of these entities without batching leads to N+1 query problems.
- **Environment Constraint:** Local environment runs Java 21, but project requires Java 25 (`modulith-kernel` depends on Java 25 features). This prevents local compilation and testing (`mvn compile` fails).
- **Optimization Strategy:** Apply `@BatchSize(size = 20)` to these collections. This allows Hibernate to fetch related entities in batches (using `IN` clause) instead of individual queries, reducing database roundtrips significantly.

**Action:**
- Apply `@BatchSize` annotation.
- Rely on code review and CI for verification due to local environment limitations.
