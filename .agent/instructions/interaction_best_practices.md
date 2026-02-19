# Antigravity Interaction Best Practices

To get the most out of your AI coding assistant and move from reactive fixes to proactive architectural stability, follow these guidelines:

## 1. Technical Anchoring (Provide the "Why" and "How")
When reporting an issue, always anchor your request in technical evidence.
- **Good**: "The `modulith-service` is failing with a 401 on `tenant1-test`. Here are the logs: [LOG BITS]. Fix it."
- **Better**: "Check the `tenants` table for credentials. If they are missing, generate them via `bench execute` on ERPNext and update the DB."
- **Why**: Providing logs or CLI output allows me to skip exploration and move directly to execution.

## 2. Strategic Directives (Think Architecturally)
Don't just ask to fix a single error. Ask for the underlying solution.
- **Strategy**: Instead of "Fix the port 8000 error," ask "Standardize the ERPNext Nginx configuration so it handles multi-tenancy correctly without manual intervention."
- **Impact**: This leads to a stabilized system rather than a series of patches.

## 3. Validation Loops (Pre-Flight Checks)
Before performing destructive or complex actions, ask for a "Health Check" or a "Pre-Flight" script.
- **Example**: "Before I deploy the storefront, write a script that verifies the Java API endpoints and ERPNext connectivity for all 4 tenants."
- **Outcome**: Catch configuration errors (like Redis host mismatches) before they impact production stability.

## 4. Workflow Maintenance
The `.agent/workflows` directory is the system's "Source of Truth" for operations. 
- **Instruction**: Antigravity is instructed to **proactively update** the relevant workflows (e.g., `fashion_ecommerce_poc.md`) after every major stabilization or architectural change without being explicitly asked.
- **Benefit**: Ensures that the knowledge is persisted and the system remains self-documenting for any future developer or agent session.

## 5. Architectural Reviews
Leverage my ability to see the "Big Picture" across modules.
- **Ask**: "Review the interaction between `SyncScheduler` and `ERPNextProductSyncService`. Is there a risk of race conditions in a multi-tenant environment?"
- **Ask**: "Compare my `docker-compose.yml` with the one in ERPNext. Are there any resource limit imbalances?"

## 6. Tech Stack Compliance
- **Rule**: Ensure all Java code is compatible with Java 25.
- **Rule**: Ensure all Spring Boot configurations are compatible with Spring Boot 4.0.2.
- **Rule**: Never downgrade the Java or Spring Boot versions.

## 7. The Scientific Debugging Protocol (Mandatory)
To prevent regression and "guess-driven development," the agent must follow this strict cycle for every bug fix:
1.  **Think & Hypothesize**: State the problem and potential causes clearly before touching any code.
2.  **Confirm Root Cause**: Use tools (`grep`, `log` analysis, `debugger`) to prove *why* the error is happening. **Do not assume.**
3.  **Propose with Evidence**: Explain the fix and back it up with the data found in step 2.
4.  **Execute**: Apply the code change.
5.  **Validate**: Immediately run a test, build, or script to verify the fix works and didn't break anything else.

## 8. Quality Assurance (Mandatory)
- **Rule**: Follow the "Test-First" Mandate (TDD) for all bug fixes and new features.
- **Rule**: Perform Impact Analysis (Blast Radius Check) before modifying shared modules.
- **Rule**: Adhere to strict Code Quality & Modernity standards (Java 25, no legacy patterns).
- **Rule**: Ensure Security & Resilience (Input Validation, Error Handling, Timeouts).
- **Rule**: Meet the Definition of Done (DoD) for every task.

## 9. Knowledge Base & Documentation Context
- **Rule**: The Agent must operate with the assumption that the documentation sets listed in `knowledge_base_rules.md` are the "Source of Truth".
- **Rule**: All code generation, debugging, and architectural advice must align with the patterns described in these resources.
- **Rule**: Official documentation overrides any "common knowledge" from older versions.
- **Rule**: Ensure all 3rd party libraries are compatible with Spring Boot 4.0.2.

---

**By following these practices, you transform Antigravity from a 'Coder' into a 'Lead Engineer' who manages the stability and scalability of your entire platform.**
