package com.app.core.multitenancy;

/**
 * Thread-safe context for holding the current tenant identifier.
 * Uses InheritableThreadLocal to ensure the tenant context is propagated to
 * child threads (e.g., Virtual Threads).
 */
public class TenantContext {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TenantContext.class);

    private static final InheritableThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();

    public static void setTenantId(String tenantId) {
        log.debug("Setting tenant context to: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
