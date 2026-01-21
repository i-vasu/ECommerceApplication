# Modern Stack Compliance: Java 25, Spring 7, Spring Boot 4

All implementation and architectural decisions MUST align with the latest standards of the 2026 Java ecosystem:

## 1. Java 25 (LTM) Standards
- **Virtual Threads**: Prefer Virtual Threads (`Thread.ofVirtual()`) for all I/O-bound tasks.
- **Pattern Matching**: Utilize full pattern matching for `switch` and `instanceof`.
- **Scoped Values & Structured Concurrency**: Use these for managing thread-local-like data and concurrent task groups in a safer, more performant way.
- **String Templates**: Use String Templates for building complex strings (HTML, SQL, JSON) where available.
- **Records**: Use Records for all DTOs and immutable data carriers.

## 2. Spring Framework 7.0 / Spring Boot 4.0 Standards
- **RestClient**: Use the synchronous `RestClient` (introduced in Spring 6.1/Boot 3.2, now the standard in Boot 4) instead of `RestTemplate`.
- **RestTestClient**: Preferred for integration testing due to its fluent API and support for both reactive and servlet stacks.
- **AOT & GraalVM**: Ensure all code is compatible with Ahead-Of-Time (AOT) compilation and native images.
- **Spring Modulith**: Strict adherence to modular monolith principles, ensuring clean boundaries between domain modules.
- **Observation API**: Use Micrometer Registration/Observation for tracing instead of legacy Sleuth/Logging patterns.

## 3. Reference Documentation (Contextual Links)
*Note: These are the canonical references to be followed.*
- [JDK 25 Documentation](https://docs.oracle.com/en/java/javase/25/)
- [Spring Framework 7.0 Reference](https://docs.spring.io/spring-framework/reference/7.0.x/)
- [Spring Boot 4.0 Reference](https://docs.spring.io/spring-boot/docs/4.0.x/reference/html/)

---
**Instruction**: Always verify compilation against Java 25 and leverage Spring Boot 4's streamlined configuration and auto-configurations. Mocking and testing should prioritize `RestTestClient`  patterns.
