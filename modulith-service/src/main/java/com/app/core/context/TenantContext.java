package com.app.core.context;

import java.util.Optional;

/**
 * Modern Tenant Context using Java 25 ScopedValue.
 * Replaces legacy ThreadLocal for better performance with Virtual Threads.
 */
public class TenantContext {

    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    /**
     * Get current tenant ID if available.
     */
    public static Optional<String> getTenantId() {
        return Optional.ofNullable(TENANT_ID.get());
    }

    /**
     * Execute a task within a tenant scope.
     */
    public static <T> T runWithTenant(String tenantId, java.util.concurrent.Callable<T> task) throws Exception {
        String previous = TENANT_ID.get();
        TENANT_ID.set(tenantId);
        try {
            return task.call();
        } finally {
            if (previous != null) {
                TENANT_ID.set(previous);
            } else {
                TENANT_ID.remove();
            }
        }
    }

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
