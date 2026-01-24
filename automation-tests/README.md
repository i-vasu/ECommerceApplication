# Automation Test Suite

This folder contains the consolidated testing suite for the E-Commerce Application.

## Technologies
- **E2E/UI**: Playwright for Java
- **API/Regression**: RestAssured
- **Unit/Integration**: JUnit 5 + Mockito
- **Architecture**: ArchUnit
- **Db Integration**: Testcontainers

## How to Run

### Run All Tests
```bash
mvn test -pl automation-tests
```

### Run Specific Suite
*   **Security Regression**: `mvn test -pl automation-tests -Dtest=SecurityRegressionTest`
*   **Order/Commerce Flow**: `mvn test -pl automation-tests -Dtest=CommerceFlowTest`
*   **UI Tests**: `mvn test -pl automation-tests -Dtest=OrderE2ETest`
*   **Order Integration**: `mvn test -pl order-service -Dtest=OrderServiceIntegrationTest`

## Folder Structure
*   `automation-tests/src/test/java/com/app/tests/e2e`: Implementation of full-system verification.
*   `order-service/src/test/java/com/app/integration`: Testcontainers for DB integration.
