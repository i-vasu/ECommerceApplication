# Task List: implementations

- [x] **Assessment & Revert** <!-- id: 0 -->
    - [x] Evaluate Migration Feasibility <!-- id: 1 -->
    - [x] Decision: **SKIP Migration** <!-- id: 2 -->
    - [x] Revert Migration Changes <!-- id: 3 -->
    - [x] Decoupling Verification (Initial) <!-- id: 4 -->
- [x] **Audit & Planning** <!-- id: 5 -->
    - [x] Audit Module Coupling <!-- id: 6 -->
    - [x] Create Refactoring Plan (`SYSTEM_AUDIT_REPORT.md`) <!-- id: 7 -->
- [x] **Refactor Event Listeners (Decoupling)** <!-- id: 8 -->
    - [x] Implement `LogisticsEventListener` (Logistics Module) <!-- id: 9 -->
    - [x] Implement `FinanceEventListener` (Finance Module) <!-- id: 10 -->
    - [x] Implement `ERPEventListener` (ERP-Sync Module) <!-- id: 11 -->
    - [x] Fix compilation errors in `modulith-service` tests.
- [ ] Verify `ServiceContextTest` passes.
- [ ] Verify `ArchitectureTest` passes.
- [!] **SKIP FQN Refactoring** (as per latest user request).
- [ ] Verify happy path: Order Placement.
