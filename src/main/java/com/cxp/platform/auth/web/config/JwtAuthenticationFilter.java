package com.cxp.platform.auth.web.config;

import com.cxp.platform.auth.application.JwtService;
import com.cxp.platform.auth.application.UserPrincipal;
import com.cxp.platform.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Security filter that authenticates API requests using bearer JWT tokens.
 *
 * It validates token integrity/expiry, maps claims to a {@link UserPrincipal},
 * populates {@link SecurityContextHolder}, and initializes tenant context for
 * tenant-scoped application services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    /**
     * Authenticates incoming requests when a bearer token is provided.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param filterChain remaining filters in the chain
     * @throws ServletException when servlet processing fails
     * @throws IOException when I/O fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean tenantBound = false;
        try {
            String authorization = request.getHeader(AUTHORIZATION_HEADER);
            if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            if (!jwtService.isTokenValid(token)) {
                log.warn("jwt_authentication_failed path={} reason=invalid_token", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            String tokenType = jwtService.extractTokenType(token);
            if (!"ACCESS".equals(tokenType)) {
                log.warn("jwt_authentication_failed path={} reason=invalid_token_type tokenType={}",
                        request.getRequestURI(),
                        tokenType);
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId;
            UUID tenantId;
            List<String> roles;
            try {
                userId = jwtService.extractUserIdAsUuid(token);
                tenantId = jwtService.extractTenantIdAsUuid(token);
                roles = jwtService.extractRoles(token);
            } catch (IllegalArgumentException ex) {
                log.warn("jwt_authentication_failed path={} reason=invalid_claim_format", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            UserPrincipal principal = UserPrincipal.builder()
                    .userId(userId)
                    .tenantId(tenantId)
                    .phone(jwtService.extractPhone(token))
                    .roles(roles)
                    .build();

            List<GrantedAuthority> authorities = roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            TenantContext.set(tenantId);
            tenantBound = true;

            filterChain.doFilter(request, response);
        } finally {
            if (tenantBound) {
                TenantContext.clear();
            }
        }
    }
}
