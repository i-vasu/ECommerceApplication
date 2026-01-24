# Compilation Fix Plan

## Objective
Fix compilation errors in the `modulith-service` module to ensure a successful build.

## Tasks
1. **Fix `BytecodeArchitectureTest.java`**
   - Error: `asInternalName().stringValue()` - `stringValue()` is undefined for `String`.
   - Action: Remove `.stringValue()` as `asInternalName()` returns a `String`.

2. **Verify `TenantContext.java` Fix**
   - Ensure `ScopedValue.call` usage is correct with `CallableOp`.

3. **Resolve `ProductDataFlowService.java` Imports**
   - Verify if resolution errors are due to compilation failure or missing dependencies.
   - MapStruct generated mappers (`CartMapperImpl`, etc.) rely on successful compilation.

4. **Verify Build**
   - Run `mvn clean compile`.
