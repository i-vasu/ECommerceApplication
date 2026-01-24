# Project Context & Agent Instructions

This repository is part of a **Fashion Store** platform with a headless architecture.

## Architecture
1. **ERPNext (Platform)**: Located at `../erpnext-platform/`. Handles all master data (Items, Customers, Inventory).
2. **Java Middleware (Backend)**: Current directory. Processes business logic and acts as an adapter for ERPNext.
3. **Next.js Storefront (Frontend)**: Located at `../vaabhi-storefront/`. The customer-facing UI.

## Working with this project
- **ERPNext Integration**: Logic is in `ERPNextService.java`. It polls ERPNext every 60s.
- **Port Mapping**: DO NOT CHANGE
  - ERPNext: 8000
  - Java: 8080
  - Next.js: 3000
- **Git Strategy**: Follow `.agent/workflows/branching_strategy.md`. Use `develop` for daily work.
- **CI/CD**: GitHub Actions are configured in `.github/workflows/`.

## Critical Pillars (SOPs)

### 1. Visual Integrity & Premium Design
- **Fashion First**: This is a premium brand. Design MUST be elegant.
- **Rules**: Use Inter/Outfit fonts, generous whitespace, and smooth transitions (Framer Motion).
- **No Defaults**: Never use default browser styles for buttons, inputs, or headers.

### 2. "Don't Break the Loop" Rule
- **End-to-End Testing**: Before pushing any change to Java or ERPNext config, verify that the data loop (ERPNext -> Java -> Storefront) is still functional.
- **Validation**: Ensure `GET /api/public/products` returns the expected DTO structure with images.

### 3. API Contract & Type Safety
- **Sync Changes**: If you modify a Java Entity or DTO that is exposed via API, you MUST immediately update the corresponding TypeScript interface in the storefront (`lib/types.ts` or similar).
- **Stability**: Avoid breaking shifts in the JSON response structure to prevent storefront crashes.

### 4. Automated Quality Gates
- **Local Testing**: Agents MUST run `mvn test` (Backend) or `npm test` (Frontend) locally and ensure they pass before pushing.
- **CI/CD Alignment**: Every push triggers a GitHub Action that spins up a PostgreSQL container. Code that fails tests will NOT be merged to `main`.
- **Timezone Note**: If tests fail with "TimeZone" errors on Windows, ensure `pom.xml` has the `Asia/Kolkata` force-fix in the Surefire plugin.

## Change Tracking & Commits
- **Change Log**: For every push to Git, the agent MUST update `CHANGELOG.md` in the root directory.
- **Format**: Include the Date, Branch Name, and a clear bulleted list of changed files/logic.
- **Commit Messages**: Use descriptive conventional commit prefixes (e.g., `feat:`, `fix:`, `refactor:`).

## Important Files
- `TODO.md`: Current project roadmap and status.
- `CHANGELOG.md`: History of all changes pushed by agents.
- `src/main/resources/application.properties`: Configuration and Env variable mapping.
- `.agent/workflows/fashion_ecommerce_poc.md`: Original setup instructions.
