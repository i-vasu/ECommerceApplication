# Engineering Quality Standards & Protocols

To ensure "Senior Engineer" level output, the agent must adhere to these quality gates for every task.

## 1. The "Test-First" Mandate (TDD)
- **Bug Fixes**: Before fixing a bug, you MUST attempt to write a reproduction test case (Unit or Integration) that fails.
  - *Exception*: Configuration or environment issues (e.g., Docker ports) where Java tests don't apply.
- **New Features**: Define the Interface/DTOs first, then write a test that asserts the expected behavior, *then* implement the logic.

## 2. Impact Analysis (The "Blast Radius" Check)
- Before modifying a `public` method, class, or API in `modulith-kernel` or any shared module:
  - Run `find_usages` to identify all dependents.
  - Verify that the change is backward compatible or plan for a refactor of all call sites.

## 3. Code Quality & Modernity
- **Java 25 Compliance**: strictly follow `modern_stack_compliance.md`.
- **No Legacy Patterns**:
  - No `System.out.println` -> Use `log.info/error`.
  - No `e.printStackTrace()` -> Log the exception properly.
  - No `Date`/`Calendar` -> Use `java.time.*`.
  - No `null` returns for collections -> Return `List.of()`.

## 4. Security & Resilience
- **Input Validation**: All DTOs must have Jakarta Validation annotations (`@NotNull`, `@Size`).
- **Error Handling**: Never swallow exceptions. Throw custom exceptions or let global handlers manage them.
- **Timeouts**: All external calls (HTTP, DB, Redis) must have configured timeouts.

## 5. Definition of Done (DoD)
A task is ONLY complete when:
1.  The code is written.
2.  The tests pass (or the fix is verified via script).
3.  The build is stable (`mvn clean compile` works).
4.  Relevant documentation (ADRs, Workflows) is updated.
