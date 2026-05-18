package com.cxp.platform.auth.application;

import java.util.List;
import java.util.UUID;

/**
 * Provides authenticated user context for request-scoped application workflows.
 *
 * This abstraction isolates use-case services from Spring Security APIs while
 * exposing identity and tenant claims required for audit, authorization, and
 * multi-tenant isolation.
 */
public interface CurrentUserProvider {

    /**
     * Returns the authenticated user identifier.
     */
    UUID getCurrentUserId();

    /**
     * Returns the authenticated governing body (tenant) identifier.
     */
    UUID getCurrentTenantId();

    /**
     * Returns the authenticated user's phone claim.
     */
    String getCurrentUserPhone();

    /**
     * Returns the authenticated user's role claims.
     */
    List<String> getCurrentUserRoles();
}
