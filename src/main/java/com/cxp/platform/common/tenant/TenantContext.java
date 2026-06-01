package com.cxp.platform.common.tenant;

import java.util.UUID;

/**
 * Thread-local governing body (tenant) identifier for the current request.
 *
 * ── Architectural rule ────────────────────────────────────────────────────────
 *
 * CITIZEN FLOWS MUST NEVER CALL TenantContext.get().
 *
 * Citizens authenticate via Supabase JWT which carries no governing_body_id claim.
 * TenantContext is intentionally left unbound for citizen requests. Any code on a
 * citizen-reachable path that calls get() will throw and produce a 400 — a silent,
 * hard-to-diagnose regression.
 *
 * Rule of thumb:
 *   TenantContext.get()      — operator/admin paths only (platform JWT, tenantId present)
 *   TenantContext.getOrNull() — any path that may be reached by a citizen
 *
 * This design supports two rollout modes:
 *   1. Community/ward MVP  — citizens operate independently; no governing body required.
 *   2. Institutional mode  — operators log in with platform JWTs carrying tenantId.
 *
 * Both modes must coexist. Citizen flows must never break because a governing body
 * has not yet been onboarded or linked.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CONTEXT = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * Returns the current governing body ID.
     * Throws IllegalStateException if not set.
     * Use ONLY on operator/admin paths where TenantContext is guaranteed to be bound.
     */
    public static UUID get() {
        UUID id = CONTEXT.get();
        if (id == null) {
            throw new IllegalStateException("Tenant context is not set for current thread");
        }
        return id;
    }

    /**
     * Returns the current governing body ID, or null if not set.
     * Safe to call on any path including citizen flows.
     */
    public static UUID getOrNull() {
        return CONTEXT.get();
    }

    public static void set(UUID id) {
        CONTEXT.set(id);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
