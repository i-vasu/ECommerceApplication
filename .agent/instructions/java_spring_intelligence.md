# Java/Spring Context Intelligence (Advanced)

To achieve "IntelliJ Ultimate" depth in Java/Spring Boot development, the agent MUST follow these specialized diagnostic and implementation rules:

## 1. Context Exploration (The Bean Graph)
Before refactoring or debugging a service, perform a wide-angle scan of the Spring Context:
- **Rule**: Use `grep_search` to find all `@Component`, `@Service`, `@Repository`, and `@Configuration` classes.
- **Rule**: Identify `@Autowired` or constructor-injected dependencies to build a mental map of the Bean Dependency Graph.
- **Rule**: Recognize `@Proxy` or `@Aspect` wrapped beans to account for non-linear execution.

## 2. API Surface Discovery (Endpoint Catalog)
For any module change, understand the exposed API surface:
- **Rule**: Catalog all `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc., in the module.
- **Rule**: Identify the DTOs used for request/response bodies and verify their field constraints (`@NotNull`, `@Size`).
- **Rule**: Cross-reference with `SecurityConfig` to identify the required roles/permissions for each endpoint.

## 3. AOP & Transactional Boundary Awareness
Understand the "invisible" logic managing data integrity:
- **Rule**: Identify `@Transactional` methods and their propagation/isolation settings.
- **Rule**: Locate `@Aspect` classes and their Pointcuts to understand where logging, caching, or auditing is applied.
- **Rule**: Ensure that exceptions thrown within `@Transactional` boundaries will correctly trigger rollbacks.

## 4. Configuration & Environment Mapping
Trace configuration properties back to their source:
- **Rule**: Map `@Value` and `@ConfigurationProperties` to `.env`, `application.yml`, or system properties.
- **Rule**: Validate that all required properties for a bean are present in the environment before suggesting execution.

## 5. Persistence & Data JPA Validation
Maintain integrity between the object model and the database:
- **Rule**: Cross-reference `@Entity` mappings with the actual database schema (`\d table_name`).
- **Rule**: Validate Spring Data JPA Repository method names (e.g., `findByTenantIdAndActiveTrue`) against Entity fields.
- **Rule**: Verify that database indexes match the query patterns used in repositories.

## 6. Runtime Visibility (Actuator)
If available, leverage Spring Actuator for live diagnostics:
- **Rule**: Query `/actuator/beans`, `/actuator/mappings`, and `/actuator/env` to confirm the actual state of the running application.

## 7. Tech Stack Compliance
- **Rule**: Ensure all Java code is compatible with Java 25.
- **Rule**: Ensure all Spring Boot configurations are compatible with Spring Boot 4.0.2.
- **Rule**: Never downgrade the Java or Spring Boot versions.

---

**By strictly following these rules, Antigravity operates with the semantic depth of a high-end IDE, ensuring typesafety, security, and architectural integrity.**
