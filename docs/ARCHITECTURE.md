# Module Architecture Guide

## Overview

This document describes the modular architecture of the Vaabhi e-commerce application and the mechanisms in place to enforce architectural boundaries.

## Module Structure

The application is organized into **6 functional modules** plus a parent aggregator:

```
fashion-store-parent (root POM)
├── modulith-kernel       - Shared infrastructure
├── modulith-identity     - User & Identity domain
├── modulith-product      - Product & Catalog domain
├── modulith-order        - Order & Commerce domain
├── modulith-service      - Main executable application
└── automation-tests      - Integration tests
```

### Module Dependencies

The dependency hierarchy flows as follows:

```
modulith-kernel (no domain dependencies)
    ↑
    ├─ modulith-identity (depends on kernel only)
    │      ↑
    ├─ modulith-product (depends on kernel + identity)
    │      ↑
    └─ modulith-order (depends on kernel + identity + product)
           ↑
    modulith-service (orchestrates all modules)
```

**Key Principle**: Dependencies flow **upward** only. Lower-level modules never depend on higher-level modules.

## Cross-Module Communication

### Service Contracts

To enable cross-module communication without tight coupling, we use **contract interfaces** defined in `modulith-kernel`:

#### Available Contracts

**`com.app.core.contracts.UserServiceContract`**
- Enables other modules to query user information
- Implemented by `modulith-identity`
- Used by `modulith-order` and `modulith-product`

**`com.app.core.contracts.EmailServiceContract`**
- Enables modules to send emails
- Implemented by `modulith-identity`
- Used by `modulith-order`

**`com.app.core.contracts.MarketingServiceContract`**
- Enables modules to trigger marketing campaigns
- Implemented by `modulith-identity`
- Used by `modulith-order`

### Event-Driven Communication

For async cross-module communication, use domain events:

**`OrderPaidEvent`** (`com.app.core.events.OrderPaidEvent`)
- Published by: `modulith-order`
- Consumed by: `modulith-identity` (MarketingEventListener)

**`USER_REGISTERED`** (via Redis Streams)
- Published by: `modulith-identity`
- Consumed by: `modulith-order` (CartEventListener)

## Architecture Testing with ArchUnit

### Overview

We use **ArchUnit** to automatically enforce architectural rules. Tests are located in:

```
modulith-service/src/test/java/com/app/architecture/
├── ModulithArchitectureTest.java
└── LayeredArchitectureTest.java
```

### Running Architecture Tests

```bash
cd modulith-service
mvn test -Dtest=ModulithArchitectureTest
mvn test -Dtest=LayeredArchitectureTest
```

### Enforced Rules

#### Module Boundary Rules

1. **No Cyclic Dependencies**
   - Modules must not depend on each other in a cycle
   - Enforced by: `modulesShouldBeFreeOfCycles()`

2. **Kernel Independence**
   - `modulith-kernel` must not depend on any domain module
   - Enforced by: `kernelShouldNotDependOnDomainModules()`

3. **Identity Independence**
   - `modulith-identity` must not depend on `order` or `product`
   - Enforced by: `identityShouldNotDependOnOrderOrProduct()`

4. **Product Independence**
   - `modulith-product` must not depend on `order`
   - Enforced by: `productShouldNotDependOnOrder()`

#### Layered Architecture Rules

1. **Controllers → Services**
   - Controllers may only access Services layer
   
2. **Services → Repositories**
   - Only Services may access Repositories

3. **No Layer Skipping**
   - Controllers cannot directly access Repositories

## Best Practices

### When Adding New Features

1. **Identify the Correct Module**
   - User management → `modulith-identity`
   - Product catalog → `modulith-product`
   - Orders/payments → `modulith-order`
   - Infrastructure → `modulith-kernel`

2. **Avoid Direct Dependencies**
   - Use contract interfaces for synchronous calls
   - Use events for asynchronous communication
   - Never import service implementations from other modules

3. **Expose via DTOs**
   - Never expose entities across module boundaries
   - Create DTOs in each module's `payloads` package
   - Use MapStruct for DTO mapping

4. **Run Architecture Tests**
   ```bash
   mvn test -Dtest=*ArchitectureTest
   ```

## Questions?

For architecture decisions, consult the team lead or refer to:
- `/task.md` - Implementation roadmap
- ArchUnit test failures - Immediate feedback on violations
