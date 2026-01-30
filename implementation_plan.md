# Implementation Plan: Lombok Removal & JDBC Migration

# Goal
Migrate the entire application from **JPA/Hibernate** to **Spring Data JDBC** and remove **Lombok** dependencies. This architectural shift decouples the domain model from ORM complexities and makes data access explicit.
Also replace JPA State Machine persistence with Redis.

## User Review Required
> [!WARNING]
> This is a destructive migration. All JPA annotations (`@Entity`, `@OneToMany`) will be removed. Code relying on "lazy loading" or "dirty checking" will break and must be refactored to explicit `save()` calls.
> **State Machine**: Switching to **Redis** for state persistence (replaces `spring-statemachine-data-jpa`). Ensure Redis is available in all environments.

## Proposed Changes

### Core Infrastructure (`modulith-kernel`)
#### [MODIFY] [pom.xml](file:///home/alway/projects/Vasu/ECommerceApplication/modulith-kernel/pom.xml)
- Remove `spring-boot-starter-data-jpa`
- Add `spring-boot-starter-data-jdbc`
- Replace `spring-statemachine-data-jpa` with `spring-statemachine-data-redis` (or custom config).

#### [MODIFY] [StateMachinePersistenceConfig.java](file:///home/alway/projects/Vasu/ECommerceApplication/modulith-governance/src/main/java/com/app/governance/states/StateMachinePersistenceConfig.java)
- Configure `RedisStateMachineRepository` instead of `JpaStateMachineRepository`.

### Catalog Module (`modulith-catalog`)
#### [MODIFY] [Product.java](file:///home/alway/projects/Vasu/ECommerceApplication/modulith-catalog/src/main/java/com/app/catalog/entities/Product.java)
- Remove Lombok. Add Manual Getters/Setters.
- Replace `@Entity`, `@OneToMany` with `@Table`, `@MappedCollection`.
- Change `Category category` -> `Long categoryId`.

#### [MODIFY] [ProductRepo.java](file:///home/alway/projects/Vasu/ECommerceApplication/modulith-catalog/src/main/java/com/app/catalog/repositories/ProductRepo.java)
- Extend `ListCrudRepository` instead of `JpaRepository`.
- Rewrite JPQL `@Query` to native SQL or JDBC methods.

### Order Module (`modulith-order`)
#### [MODIFY] [Order.java](file:///home/alway/projects/Vasu/ECommerceApplication/modulith-order/src/main/java/com/app/order/entities/Order.java)
- Remove Lombok.
- Convert `List<OrderItem>` to JDBC aggregate style.
- Explicitly manage `shippingAddress` components if currently `@Embedded` (JDBC supports `@Embedded(onEmpty = ...)`, checking checking support).

#### [MODIFY] [OrderServiceImpl.java](file:///home/alway/projects/Vasu/ECommerceApplication/modulith-order/src/main/java/com/app/order/order/OrderServiceImpl.java)
- Refactor logic to load referenced entities by ID (e.g. `productRepo.findById(item.getProductId())` instead of `item.getProduct()`).

## Verification Plan
### Automated Tests
- Run `mvn clean install` to verify compilation.
- Run `mvn test` to ensure logic remains correct (repositories often mocked, integration tests crucial).

### Manual Verification
- Verify application startup (Database schema must match new mapping expectation).
- Test "Place Order" flow locally.
