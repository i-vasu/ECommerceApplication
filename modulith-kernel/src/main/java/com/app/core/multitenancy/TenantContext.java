package com.app.core.multitenancy;

import module java.base;

/**
 * Thread-safe context for holding the current tenant identifier.
 * Uses InheritableThreadLocal to ensure the tenant context is propagated to
 * child threads (e.g., Virtual Threads).
 */
public class TenantContext {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TenantContext.class);

    // Java 25 ScopedValue
    public static final ScopedValue<String> TENANT = ScopedValue.newInstance();

    public static String getTenantId() {
        return TENANT.isBound() ? TENANT.get() : "public";
    }

    /**
     * Run a Runnable within the scope of a tenant.
     */
    public static void runWithTenant(String tenantId, Runnable task) {
        ScopedValue.where(TENANT, tenantId).run(task);
    }

    /**
     * Call a Callable within the scope of a tenant.
     */
    public static <T> T callWithTenant(String tenantId, java.util.concurrent.Callable<T> task) throws Exception {
        return ScopedValue.where(TENANT, tenantId).call(task::call);
    }
}
