# Assessment: Migration to Lombok-Free & Spring Data JDBC

## 1. Executive Summary
**Feasibility:** ✅ Feasible but High Effort
**Estimated Time:** 3-5 Days
**Risk Level:** 🔴 High (Fundamental architectural shift)

## 2. Component Analysis

### A. Removing Lombok (`lombok.*`)
*   **Current State:** Heavy usage of `@Data`, `@Builder`, `@AllArgsConstructor`. However, some entities (`Order`, `Product`) already contain redundant manual getters/setters.
*   **Impact:** 
    *   **Boilerplate:** Code size will increase by ~40%.
    *   **Readability:** Noise increases, but "magic" decreases.
    *   **Refactoring:** Trivial but tedious.
*   **Recommendation:** Proceed. It aligns with the goal of explicit, dependency-free domain models.

### B. Migrating to Spring Data JDBC (from JPA/Hibernate)
*   **Current State:** Deep dependency on JPA annotations and JPQL queries.
*   **Architectural Shift:** 
    *   **JPA**: Relational mapping with hidden magic (lazy loading, dirty checking).
    *   **JDBC**: Domain-Driven Design (DDD) focused. 1:1 mapping between Aggregate Root and Repository. No session, no lazy loading.
*   **Code Impact:**
    *   **Entities:** Must replace all JPA annotations with `org.springframework.data.*`.
    *   **Relationships:** `@OneToMany` lists inside an aggregate work well. Relationships across aggregates MUST become ID references (e.g., `Category category` -> `Long categoryId`).
    *   **Repositories:** All JPQL queries must be rewritten as native SQL.
    *   **Services:** Fix compilation errors due to broken object graph traversal.
*   **Risk:** 
    *   Breaking implicit behavior (dirty checking).
    *   Performance regressions if N+1 selects aren't managed.

## 3. Implementation Plan

### Phase 1: Preparation
1.  **Stop New Feature Dev**: Freeze schema changes.
2.  **Test Suite**: Ensure all integration tests pass.

### Phase 2: Domain Modeling
1.  **Define Aggregates**:
    *   **Catalog**: `Product` (Root).
    *   **Order**: `Order` (Root) -> `OrderItem` (Child).
2.  **Refactor References**: Change Object references to IDs.

### Phase 3: Migration Execution (Per Module)
1.  **Dependencies**: Swap `spring-boot-starter-data-jpa` for `data-jdbc`.
2.  **Entities**: Strip JPA/Lombok. Add manual code.
3.  **Repositories**: Convert `JpaRepository` to `ListCrudRepository`.
4.  **Services**: Fix compilation errors.

## 4. Verdict
This migration forces **better architecture** (true DDD) but requires rewriting the persistence layer. 
