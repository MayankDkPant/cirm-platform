package com.cxp.platform.common.tenant;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CONTEXT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID id) {
        CONTEXT.set(id);
    }

    public static UUID get() {
        UUID id = CONTEXT.get();
        if (id == null) {
            throw new IllegalStateException("Tenant context is not set for current thread");
        }
        return id;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
