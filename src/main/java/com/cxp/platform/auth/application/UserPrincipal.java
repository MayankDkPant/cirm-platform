package com.cxp.platform.auth.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Immutable authenticated user model carried inside the security context.
 *
 * This principal standardizes identity and tenant claims for downstream
 * application services without coupling use-cases to JWT parsing logic.
 */
@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final String phone;
    private final UUID tenantId;
    private final List<String> roles;
}
