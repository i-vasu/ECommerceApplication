# Codebase Quality Analysis & Transformation Plan

An analysis of the `modulith-service` identifies several areas for modernization and architectural hardening.

## 1. Dependency & Architecture Pitfalls
- **Field Injection**: Widespread use of `@Autowired` on private fields (e.g., `OrderServiceImpl`, `ProductServiceImpl`).
- **Module Leakage**: Direct Repository access across modules (e.g., `Order` module using `UserRepo`).
- **Import Strategy Inconsistency**: 
    - **Mistake**: Using FQN for common types like `RestClient` inside method bodies.
    - **The Rule**: **Zero FQN**. Standard classes (e.g., `List`, `ArrayList`, `RestClient`) must be **imported** rather than used with fully qualified names.
- **Manual Mapping**: Heavy reliance on manual field-by-field copying (e.g., `CartServiceImpl.addProductToCart`).
- **Redundant DB Calls**: Finding the same entity multiple times within a single transaction because of lack of local caching or state sharing.
- **Hardcoded Logic**: Fallback URLs (e.g., `http://localhost:8000`) and configurations hardcoded in services.

## 2. Modern Java (JDK 25) & Log4j2
- **Legacy Logging**: Use of `LoggerFactory.getLogger` instead of Lombok's `@Log4j2` annotation.
- **Local Type Inference**: Underutilization of `var` in service implementations.
- **Legacy Streams**: Using `.collect(Collectors.toList())` instead of the more concise `.toList()`.
- **Collection Idioms**: Prefer `isEmpty()` over `size() == 0`.
- **Java Records**: DTOs should be `record` types (already started with `ProductDTO`).

---

## 3. Transformation Plan (Phase 0)

### Phase 0.1: Structural Foundations
1. **Lombok Implementation**: Annotate services with `@RequiredArgsConstructor` and `@Log4j2`.
2. **Constructor Injection**: Convert all `@Autowired` fields to `final` fields.
3. **Import Audit**: Standardize imports vs FQN (List exception).

### Phase 0.2: Modernization & API Cleanliness
1. **Records Migration**: Convert all DTOs (Cart, Order, User, Address) to Records.
2. **Standardize Streams**: Replace all `Collectors.toList()` with `.toList()`.
3. **Var adoption**: Implement `var` in all local scopes where readability is maintained.

### Phase 0.3: Architectural Hardening
1. **Internal APIs**: Create `module-internal` interfaces to hide Repositories.
2. **Centralized Mapping**: Ensure every DTO <-> Entity transition uses a MapStruct mapper.
3. **Config Externalization**: Move all hardcoded URLs to `@ConfigurationProperties`.
