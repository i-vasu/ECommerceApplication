# Git Branching Strategy

To maintain a clean and stable codebase, we follow a simplified **GitFlow** approach.

## 1. Branch Types

- **`main`**: 
  - Source of truth for production code.
  - Every commit here must be tagged with a version.
  - No direct commits. Only merges from `develop` or `hotfix`.

- **`develop`**: 
  - Main integration branch.
  - Features are merged here once tested.
  - Stable enough for nightly builds or staging.

- **`feature/<name>`**: 
  - Used for developing new features/logic.
  - Branch off from `develop`.
  - Merge back into `develop` via Pull Request.

- **`hotfix/<name>`**: 
  - Urgent fixes for production.
  - Branch off from `main`.
  - Merge into both `main` and `develop`.

## 2. Rule for Agents
- **Always** create a feature branch before making architectural changes.
- **Never** push sensitive data (keys, secrets) to any branch (check `.gitignore`).
- **Sync** `develop` frequently to avoid large merge conflicts.

## 3. Pull Request Guidelines
- PRs to `develop` require successful CI/CD build passing.
- Descriptive titles and summaries of changes.
