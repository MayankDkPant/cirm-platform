package com.cirm.platform.auth.application;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Security-backed implementation of {@link CurrentUserProvider}.
 *
 * This adapter reads authenticated identity and tenant information from the
 * Spring Security context, providing a stable interface for application
 * services and audit workflows.
 */
@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    /**
     * Returns current authenticated user id.
     *
     * @return authenticated user UUID
     */
    @Override
    public UUID getCurrentUserId() {
        return getRequiredPrincipal().getUserId();
    }

    /**
     * Returns current authenticated tenant id.
     *
     * @return authenticated governing body UUID
     */
    @Override
    public UUID getCurrentTenantId() {
        return getRequiredPrincipal().getTenantId();
    }

    /**
     * Returns current authenticated user phone.
     *
     * @return phone claim value from JWT
     */
    @Override
    public String getCurrentUserPhone() {
        return getRequiredPrincipal().getPhone();
    }

    /**
     * Returns current authenticated role list.
     *
     * @return list of roles carried in JWT
     */
    @Override
    public List<String> getCurrentUserRoles() {
        List<String> roles = getRequiredPrincipal().getRoles();
        return roles == null ? List.of() : List.copyOf(roles);
    }

    private UserPrincipal getRequiredPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No authenticated user found in security context");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new IllegalStateException("Security principal is not of type UserPrincipal");
        }
        return userPrincipal;
    }
}
