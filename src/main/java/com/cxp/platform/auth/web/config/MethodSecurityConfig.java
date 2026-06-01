package com.cxp.platform.auth.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables Spring method-level security globally.
 *
 * Not profile-gated so that @PreAuthorize guards are active on both
 * the "local" and production profiles. Behavior difference by profile:
 *   production  — JwtAuthenticationFilter populates roles from the Bearer token
 *   local       — DevIdentityFilter injects ROLE_CITIZEN + ROLE_OPERATOR for the dev user
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
