# Knowledge Base & Documentation Context

The Agent must operate with the assumption that the following documentation sets are the "Source of Truth". All code generation, debugging, and architectural advice must align with the patterns described in these resources.

## 1. Core Platform Documentation
- **Java 25 (LTM)**: [https://docs.oracle.com/en/java/javase/25/](https://docs.oracle.com/en/java/javase/25/)
  - *Focus*: Virtual Threads, Structured Concurrency, Scoped Values, Pattern Matching, Vector API.
- **Spring Boot 4.0.2**: [https://docs.spring.io/spring-boot/docs/4.0.2/reference/html/](https://docs.spring.io/spring-boot/docs/4.0.2/reference/html/)
  - *Focus*: AOT/Native support, Observability (Micrometer), RestClient, Docker Compose support.
- **Spring Framework 7.0**: [https://docs.spring.io/spring-framework/reference/7.0.x/](https://docs.spring.io/spring-framework/reference/7.0.x/)

## 2. Ecosystem Documentation (Compatible Versions)
- **Spring Cloud 2025.1.0 (Oakwood)**: [https://spring.io/projects/spring-cloud](https://spring.io/projects/spring-cloud)
- **Spring Modulith 2.0.2**: [https://spring.io/projects/spring-modulith](https://spring.io/projects/spring-modulith)
- **Spring Security 7.0**: [https://docs.spring.io/spring-security/reference/](https://docs.spring.io/spring-security/reference/)
- **Spring Data 2025.1.0**: [https://spring.io/projects/spring-data](https://spring.io/projects/spring-data)
- **Vaadin 25.0.3+**: [https://vaadin.com/docs](https://vaadin.com/docs) (Required for Boot 4 / Jakarta EE 11)
- **Spring AI 2.0.0-M1+**: [https://docs.spring.io/spring-ai/reference/](https://docs.spring.io/spring-ai/reference/)
- **SpringDoc 3.0.0+**: [https://springdoc.org/](https://springdoc.org/) (Required for Security 7.0)
- **Testcontainers 2.0.2+**: [https://testcontainers.com/](https://testcontainers.com/) (Native Java 25 support)

## 3. Contextual Rules
1.  **Precedence**: Official documentation overrides any "common knowledge" from older versions (e.g., prefer `RestClient` over `RestTemplate`).
2.  **Verification**: If a generated API usage seems deprecated or removed in version 4.0.2, the Agent must verify against the migration guides.
3.  **Ecosystem Alignment**: Ensure all 3rd party libraries (Hibernate, Jackson, Flyway) are compatible with Spring Boot 4.0.2.

## 4. Persistent Context
The Agent shall treat these URLs as "pinned" context. When in doubt about a configuration property or annotation, assume the Spring Boot 4.0.2 variant (e.g., `spring.jpa.open-in-view` behavior, `server.shutdown` defaults).
