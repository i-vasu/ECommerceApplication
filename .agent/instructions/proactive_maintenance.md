# Proactive Workflow Maintenance

To ensure the long-term stability and operability of the system, the agent MUST follow these instructions regarding the `.agent/workflows` directory:

## 1. Automatic Updates
- After completing a task that results in a **stabilized flow**, **architectural change**, or **new multi-tenant setup**, the agent MUST proactively review and update the corresponding workflow file in `.agent/workflows/`.
- Do not wait for the user to request a documentation update. Identifying that a flow has changed and updating the manual is a core responsibility.

## 2. Standardized Formatting
- Ensure all workflows follow the standard YAML frontmatter format.
- Use `// turbo` and `// turbo-all` annotations strategically to enable safe auto-run for future sessions.

## 3. Reliability Overlap
- If a fix is made for one tenant (e.g., Nginx config), ensure the workflow reflects how to apply that fix system-wide or how it was already generalized.

## 4. Notification
- When a workflow is updated proactively, mention it in the final `notify_user` call to ensure the user is aware of the documentation change.
