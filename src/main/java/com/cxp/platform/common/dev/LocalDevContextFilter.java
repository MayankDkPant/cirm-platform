package com.cxp.platform.common.dev;

import com.cxp.platform.auth.application.UserPrincipal;
import com.cxp.platform.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Dev-only filter that injects a fixed tenant + user context for unauthenticated
 * requests to ServiceRequest/Complaint endpoints, enabling end-to-end local testing
 * without a fully implemented OTP/JWT flow.
 *
 * TenantContext scope:
 *   - ServiceRequest.create() no longer reads TenantContext — governing body is derived
 *     from GPS coordinates via MockWardLookupService. The injected tenantId is harmless
 *     for that path but still required for list()/get()/updateStatus(), which are
 *     operator-scoped and filter by TenantContext.get().
 *
 * TODO: Remove this filter (and LocalDevConfiguration) once OTP verification and
 *       JWT issuance are complete. Tracked in: auth flow completion.
 *
 * Active only when the "local" Spring profile is enabled.
 */
@Slf4j
public class LocalDevContextFilter extends OncePerRequestFilter {

    private static final UUID DEV_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private final JdbcTemplate jdbcTemplate;
    private volatile UUID cachedDevTenantId;

    public LocalDevContextFilter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (hasBearerToken(request) || !isServiceRequestPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID tenantId = resolveDevTenantId();

        UserPrincipal principal = UserPrincipal.builder()
                .userId(DEV_USER_ID)
                .tenantId(tenantId)
                .phone("0000000000")
                .roles(List.of("CITIZEN"))
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        TenantContext.set(tenantId);

        log.debug("dev_context_injected path={} tenantId={}", request.getRequestURI(), tenantId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private UUID resolveDevTenantId() {
        if (cachedDevTenantId == null) {
            synchronized (this) {
                if (cachedDevTenantId == null) {
                    cachedDevTenantId = jdbcTemplate.query(
                            "SELECT id FROM governing_body WHERE is_active = TRUE ORDER BY created_at LIMIT 1",
                            rs -> rs.next() ? rs.getObject(1, UUID.class) : null
                    );
                    if (cachedDevTenantId == null) {
                        throw new IllegalStateException(
                                "No active governing_body found. Run Flyway migrations and seed data first.");
                    }
                    log.warn("LOCAL DEV: resolved dev tenant id={} — this filter must not run in production", cachedDevTenantId);
                }
            }
        }
        return cachedDevTenantId;
    }

    private boolean hasBearerToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return auth != null && auth.startsWith("Bearer ");
    }

    private boolean isServiceRequestPath(String path) {
        return path.startsWith("/api/v1/service-requests")
                || path.startsWith("/api/v1/complaints");
    }
}
