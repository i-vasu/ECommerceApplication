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

---

**By following these practices, you transform Antigravity from a 'Coder' into a 'Lead Engineer' who manages the stability and scalability of your entire platform.**
