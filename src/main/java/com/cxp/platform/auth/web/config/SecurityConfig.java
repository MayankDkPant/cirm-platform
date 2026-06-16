package com.cxp.platform.auth.web.config;

import com.maadhyam.logging.filter.RequestTraceFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Production security configuration — active on all profiles EXCEPT "local".
 *
 * Route authorization is organized into three explicit sections:
 *
 *   ① PUBLIC          No authentication required. Every entry here widens the
 *                      platform's public surface and requires explicit governance review.
 *
 *   ② API (/api/**)   Valid Bearer JWT required: Supabase ES256 (citizen) or Platform
 *                      HS256 (operator). Citizen-vs-operator distinction is enforced in
 *                      the service layer via TenantContext.getOrNull(). Method-level role
 *                      enforcement (@PreAuthorize) is not yet declared — planned for Wave 0.
 *
 *   ③ OUT-OF-NAMESPACE  Any route not matched above. Currently permit-all to avoid
 *                      breaking known out-of-namespace routes (POST /ai/chat).
 *                      Targeted for replacement with denyAll() in Wave 0 / PR2.
 *
 * No runtime behavior is changed by this file's organization — only governance visibility.
 * See LocalDevSecurityConfig for the mirror configuration used under the "local" profile.
 */
@Configuration
@Profile("!local")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RequestTraceFilter requestTraceFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth

                        // ── ① PUBLIC ROUTES ──────────────────────────────────────────────────────
                        // No authentication required.
                        // Adding entries here widens the platform's public surface — each requires
                        // explicit governance approval and documentation in the OpenAPI spec.

                        // Auth bootstrap: "/auth/**" is a legacy alias kept for backward compatibility.
                        // "/api/v1/auth/**" covers OTP request (dormant — local profile only, not in
                        // production) and any future auth flows added under this prefix.
                        // "/actuator/health" is the infrastructure health probe for load balancers / k8s.
                        //
                        // OpenAPI / Swagger UI — documentation endpoints, not data endpoints.
                        // "/swagger-ui/**" and "/v3/api-docs/**" are read-only and carry no
                        // sensitive runtime data. Exposing them publicly is intentional and
                        // necessary for SDK generation and FE integration workflows.
                        // Local/dev controllers (@Profile("local")) are absent from the Spring
                        // context in production, so they do not appear in the generated spec.
                        .requestMatchers(
                                "/auth/**", "/api/v1/auth/**", "/actuator/health",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
                        ).permitAll()

                        // Anonymous civic announcement feed — returns CitizenFeedResponse (no internal fields).
                        // Exact path only. Any sub-path of /feed (e.g. /feed/admin) and all
                        // POST/PATCH on /api/v1/announcements/** fall through to section ② and require auth.
                        .requestMatchers(HttpMethod.GET, "/api/v1/announcements/feed").permitAll()

                        // ── ② API ROUTES — Bearer JWT required ───────────────────────────────────
                        // Platform JWT (operator, carries tenantId claim) or Supabase JWT (citizen,
                        // no tenantId). JwtAuthenticationFilter validates both formats and populates
                        // SecurityContextHolder + TenantContext before this rule is evaluated.
                        //
                        // Citizen endpoints (Supabase JWT — no tenantId claim):
                        //   POST   /api/v1/users/provision                profile create / update
                        //   GET    /api/v1/users/me                       authenticated citizen profile
                        //   POST   /api/v1/service-requests               submit a service request
                        //   GET    /api/v1/service-requests               list citizen's own requests
                        //   GET    /api/v1/service-requests/{id}          single request
                        //   GET    /api/v1/service-requests/{id}/events   event timeline
                        //   POST   /api/v1/service-requests/analyze       AI enrichment preview (pre-submit)
                        //   GET    /api/v1/announcements/my-feed          personalized civic feed
                        //   GET    /api/v1/announcements/{id}             single announcement detail
                        //
                        // Operator endpoints (Platform JWT — tenantId required, convention only):
                        //   Distinction is currently enforced in the service layer via TenantContext.
                        //   No method-level role guard is in place yet.
                        //   POST   /api/v1/announcements                  create announcement
                        //   GET    /api/v1/announcements                  operator list (all statuses)
                        //   PATCH  /api/v1/service-requests/{id}/status   status lifecycle management
                        //
                        // Deprecated endpoints (pending Wave 7 cleanup):
                        //   POST   /api/v1/ai/service-requests/analyze    duplicate of /service-requests/analyze
                        //
                        // TODO (Wave 0 / PR2): Add @PreAuthorize("hasRole('OPERATOR')") to each
                        // operator-only controller method listed above. Until then, a citizen JWT
                        // that reaches an operator path will fail at the service layer (TenantContext),
                        // not at the HTTP authorization layer — no 401/403 is returned pre-emptively.
                        .requestMatchers("/api/**").authenticated()

                        // ── ③ DENY-BY-DEFAULT ────────────────────────────────────────────────────
                        // Any route not matched by ① or ② is denied.
                        // This is the platform's canonical security posture.
                        //
                        // To expose a new route, add it explicitly to section ① (public) or ensure
                        // it is under /api/** (section ②). No route is public by default.
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(requestTraceFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
