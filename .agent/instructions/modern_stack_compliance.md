# Modern Stack Compliance: Java 25, Spring 7, Spring Boot 4.0.2

All implementation and architectural decisions MUST align with the latest standards of the 2026 Java ecosystem:

## 1. Java 25 (LTM) Standards
- **Virtual Threads**: Prefer Virtual Threads (`Thread.ofVirtual()`) for all I/O-bound tasks.
- **Pattern Matching**: Utilize full pattern matching for `switch` and `instanceof`.
- **Scoped Values & Structured Concurrency**: Use these for managing thread-local-like data and concurrent task groups in a safer, more performant way.
- **String Templates**: Use String Templates for building complex strings (HTML, SQL, JSON) where available.
- **Records**: Use Records for all DTOs and immutable data carriers.
- **Sequenced Collections**: Use `getFirst()`, `getLast()`, `addFirst()` etc. for List/Set operations.
- **Module Imports**: Use `import module java.base;` where applicable to reduce boilerplate.
- **Stream Gatherers**: Use `Gatherers.*` for complex stream operations like windowing.

## 2. Spring Framework 7.0 / Spring Boot 4.0.2 Standards
- **RestClient**: Use the synchronous `RestClient` (introduced in Spring 6.1/Boot 3.2, now the standard in Boot 4) instead of `RestTemplate`.
- **RestTestClient**: Preferred for integration testing due to its fluent API and support for both reactive and servlet stacks.
- **AOT & GraalVM / CRaC**: Ensure all code is compatible with Ahead-Of-Time (AOT) compilation and native images. Support Coordinated Restore at Checkpoint (CRaC) for instant startup.
- **Spring Modulith**: Strict adherence to modular monolith principles, ensuring clean boundaries between domain modules.
- **Observation API**: Use Micrometer Registration/Observation for tracing with OpenTelemetry.
- **API Versioning**: First-Class native versioning or standard URI-path versioning (`/api/v1`).

## 3. Verified Implementation Patterns (Project Specific)
- **Startup Speed**: Target <1s. Use `spring.jpa.hibernate.ddl-auto=none` (rely on Flyway), `spring.main.lazy-initialization=true`, and `-XX:+UseCompactObjectHeaders`.
- **Schema Management**: Flyway is the standard. Tables MUST be created via migration scripts, not Hibernate.
- **Multitenancy**: `TenantContext` is powered by `ScopedValue` (Java 25) and propagated via `TenantFilter`.
- **Architecture Validation**: Use `java.lang.classfile` API tests to enforce layering rules.

## 4. Reference Documentation (Contextual Links)
*Note: These are the canonical references to be followed.*
- [JDK 25 Documentation](https://docs.oracle.com/en/java/javase/25/)
- [Spring Framework 7.0 Reference](https://docs.spring.io/spring-framework/reference/7.0.x/)
- [Spring Boot 4.0.2 Reference](https://docs.spring.io/spring-boot/docs/4.0.2/reference/html/)

---
## 5. Optimization & Execution Patterns
- **Build**: `mvn clean install -pl modulith-service -DskipTests`
- **CDS Training**: Run [cds_train.sh](file:///home/alway/projects/Vasu/ECommerceApplication/modulith-service/scripts/cds_train.sh) or use `mvn spring-boot:run -Pcds-train`.
- **Minimized JVM Startup**: Always use the following JVM args for production-like performance:
  `-XX:SharedArchiveFile=application.jsa -XX:+UseZGC -XX:+UseCompactObjectHeaders --enable-preview`
- **CRaC Checkpoint**: Use [crac_checkpoint.sh](file:///home/alway/projects/Vasu/ECommerceApplication/modulith-service/scripts/crac_checkpoint.sh) to snapshot the JVM state.
- **CRaC Restore**: Use [crac_restore.sh](file:///home/alway/projects/Vasu/ECommerceApplication/modulith-service/scripts/crac_restore.sh) for instant startup (<500ms).

**Instruction**: Always verify compilation against Java 25 and leverage Spring Boot 4.0.2's streamlined configuration and auto-configurations. Mocking and testing should prioritize `RestTestClient` patterns. Under any circumstance, we must never change to lower versions.
