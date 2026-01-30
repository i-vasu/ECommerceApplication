# Decision Memo: Migration Necessity

## 🛑 Is it "Necessary"? 
**Technical Answer: NO.** 
The application currently functions correctly with JPA/Hibernate. The "Necessity" is purely **architectural** and **strategic**, not functional.

## The Trade-off Matrix

| Feature | Data JPA (Current) | Spring Data JDBC (Proposed) |
| :--- | :--- | :--- |
| **Development Speed** | 🚀 **Fast** (auto-mapping, magic) | 🐢 **Slower** (explicit mapping) |
| **Code "Magic"** | 🎩 **High** (Lazy loading, Dirty checking) | ⬜ **Zero** (What you see is what you get) |
| **Performance** | ⚠️ **Unpredictable** (N+1 issues, Entity Graph complexities) | ⚡ **Predictable** (SQL-centric) |
| **Domain Model** | 🕸️ **Coupled** to DB (Entities are tables) | 🏰 **Pure** (Aggregates are explicit) |
| **Refactoring Cost** | 🟢 Low (Already built) | 🔴 **High** (Rewrite all Repos/Entities) |

## Recommendation

### 1. ⏩ **KEEP CURRENT (JPA + Lombok)** if:
*   **Time-to-Market is critical**: You want to launch or demo *now*.
*   **Feature Completeness**: You still have modules to build (e.g., Frontend integration, completing the 3 remaining backend modules).
*   **Resource Constraints**: You prefer spending time on business logic rather than plumbing.

### 2. 🏗️ **MIGRATE (JDBC + No Lombok)** if:
*   **Long-Term Maintainability** is the #1 priority.
*   You strictly adhere to **Domain-Driven Design (DDD)** principles.
*   You have encountered **unsolvable performance issues** with Hibernate.
*   **"No Magic"** is a hard team rule.

### My Advice
**Postpone the migration.** 
We are currently in the middle of "Event-Based Decoupling" and "Stabilization". 
Finishing the **functional goals** (the remaining 20% of decoupling + Integration Tests) delivers more *tangible value* right now than rewriting the persistent layer.

**Proposed Revised Plan:**
1.  **Finish Event Decoupling** (Governance/Intelligence/Service are mostly done).
2.  **Add Integration Tests** (Verify the system works).
3.  **Launch/Verify**.
4.  *Then*, if the project is successful, treat "Migration to JDBC" as a "V2 Refactor".
