package com.cxp.platform.common.dev;

import com.cxp.platform.auth.application.UserPrincipal;
import com.cxp.platform.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Dev-only authentication filter — active ONLY on the "local" Spring profile.
 *
 * For requests that carry no Bearer token, injects the local-dev user identity
 * (seeded by DevUserSeeder) into the SecurityContext and TenantContext, allowing
 * all authenticated endpoints to work in local development without a real JWT flow.
 *
 * Applies to ALL request paths — no path list to maintain. Any endpoint that
 * works in production with a valid JWT will automatically work in local dev.
 *
 * If a Bearer token IS present, the filter passes through and JwtAuthenticationFilter
 * handles it normally (so manual JWT testing still works in local dev).
 *
 * If the dev user has not been seeded (DevUserSeeder failed), the filter passes
 * through without injecting identity — endpoints requiring auth will return 401,
 * which is the correct signal that the DB seed data is missing.
 *
 * To be deleted when real JWT issuance (OTP verify endpoint) is implemented.
 */
@Slf4j
@RequiredArgsConstructor
public class DevIdentityFilter extends OncePerRequestFilter {

    private final DevUserSeeder devUserSeeder;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        UUID userId   = devUserSeeder.getDevUserId();
        UUID tenantId = devUserSeeder.getDevTenantId();

        if (userId == null) {
            log.warn("dev_identity_filter: dev user not seeded — passing request unauthenticated path={}",
                    request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .tenantId(tenantId)
                .phone("dev-local")
                .roles(List.of("CITIZEN", "OPERATOR"))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_CITIZEN"),
                                new SimpleGrantedAuthority("ROLE_OPERATOR"))));

        boolean tenantBound = false;
        if (tenantId != null) {
            TenantContext.set(tenantId);
            tenantBound = true;
        }

        log.debug("dev_identity_injected path={} userId={}", request.getRequestURI(), userId);

        try {
            chain.doFilter(request, response);
        } finally {
            if (tenantBound) TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
