package com.cxp.platform.auth.web.config;

import com.cxp.platform.common.dev.DevIdentityFilter;
import com.maadhyam.logging.filter.RequestTraceFilter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Local development security configuration — active ONLY when the "local" profile is enabled.
 *
 * DevIdentityFilter is registered before JwtAuthenticationFilter and injects the dev
 * user identity for all requests that carry no Bearer token. This means every endpoint
 * that requires authentication works in local dev without needing a real JWT.
 *
 * No paths are explicitly opened with permitAll (beyond the public production rules).
 * DevIdentityFilter covers authentication for all protected endpoints.
 *
 * Delete this class once OTP verify + JWT issuance is complete and the "local" profile
 * is no longer needed for auth bypass.
 */
@Slf4j
@Configuration
@Profile("local")
public class LocalDevSecurityConfig {

    @PostConstruct
    public void logDevMode() {
        log.warn("╔══════════════════════════════════════════════════════════════╗");
        log.warn("║  LOCAL DEV SECURITY ACTIVE                                   ║");
        log.warn("║  DevIdentityFilter injects dev user on all unauthed requests ║");
        log.warn("║  DO NOT activate the 'local' profile in staging or prod.     ║");
        log.warn("╚══════════════════════════════════════════════════════════════╝");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RequestTraceFilter requestTraceFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            DevIdentityFilter devIdentityFilter) throws Exception {

        log.info("dev_identity_filter_registered filter={}", devIdentityFilter.getClass().getSimpleName());

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth

                        // ── ① PUBLIC ROUTES ──────────────────────────────────────────────────────
                        // Mirror of SecurityConfig section ①. Keep in sync.
                        .requestMatchers(
                                "/auth/**", "/api/v1/auth/**", "/actuator/health",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/announcements/feed").permitAll()

                        // Local-dev-only test utilities. @Profile("local") on the controller
                        // guarantees these routes do not exist in production — this matcher is
                        // therefore safe to leave in place without production risk.
                        .requestMatchers("/test/**").permitAll()

                        // ── ② API ROUTES — Bearer JWT (or DevIdentityFilter injection) required ──
                        // Mirror of SecurityConfig section ②. Keep in sync.
                        // In the local profile, DevIdentityFilter (registered below) injects the
                        // dev-user identity for requests that carry no Bearer token, so protected
                        // endpoints are accessible without a real JWT during development.
                        .requestMatchers("/api/**").authenticated()

                        // ── ③ DENY-BY-DEFAULT ─────────────────────────────────────────────────────
                        // Mirror of SecurityConfig section ③. Keep in sync.
                        // DevIdentityFilter covers authenticated /api/** paths above.
                        // Any other route not matched by ① or ② is denied.
                        .anyRequest().denyAll()
                )
                // devIdentityFilter runs first: injects dev identity if no Bearer token.
                // jwtAuthenticationFilter runs second: validates real Bearer tokens.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(devIdentityFilter, jwtAuthenticationFilter.getClass())
                .addFilterAfter(requestTraceFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
