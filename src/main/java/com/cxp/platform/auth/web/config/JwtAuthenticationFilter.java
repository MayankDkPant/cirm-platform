package com.cxp.platform.auth.web.config;

import com.cxp.platform.auth.application.JwtService;
import com.cxp.platform.auth.application.UserPrincipal;
import com.cxp.platform.auth.infrastructure.persistence.UserRepository;
import com.cxp.platform.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Validates Bearer JWTs and populates SecurityContextHolder + TenantContext.
 *
 * Validation order:
 *   1. Platform HS256 JWT (issued by this server via JwtService). Full claim set, includes
 *      token_type=ACCESS and optional governing_body_id for operator flows.
 *   2. Supabase JWT (issued by Supabase Auth). Validated via JWKS at the Supabase endpoint.
 *      The email claim is used to resolve the internal platform User record.
 *
 * Token handling policy:
 *   - No Bearer header → pass through unauthenticated; SecurityConfig authorization rules apply.
 *   - Bearer present but unrecognized by either decoder → log warning, pass through unauthenticated.
 *     This filter NEVER writes a 401 directly — Spring Security's authorization filter owns that.
 *   - Valid Supabase JWT, email not yet provisioned in the platform →
 *     A provisional UserPrincipal (userId=null) is set in the SecurityContext.
 *     The request IS considered externally authenticated. Services that call
 *     currentUserProvider.getCurrentUserId() throw IllegalStateException when userId is null.
 *     This allows POST /api/v1/users/provision to succeed (it reads email from the request body
 *     and does not call getCurrentUserId()). After provisioning, the user row exists and
 *     subsequent requests resolve the full UserPrincipal.
 *   - Valid token, governing_body_id absent → TenantContext not bound. This is the normal
 *     state for all Supabase (citizen) JWTs. Citizen endpoints must use TenantContext.getOrNull()
 *     and must never require a governing body to be present. Operator endpoints calling
 *     TenantContext.get() will throw for citizen tokens — this is intentional and expected.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final JwtDecoder supabaseDecoder;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            @Value("${supabase.jwks-uri:}") String jwksUri,
            @Value("${supabase.issuer-uri:}") String issuerUri) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.supabaseDecoder = buildSupabaseDecoder(jwksUri, issuerUri);

        if (this.supabaseDecoder != null) {
            log.info("supabase_jwks_decoder_initialized jwks_uri={} issuer_uri={} algorithms=ES256,RS256", jwksUri, issuerUri);
        } else {
            log.warn("supabase_jwks_decoder_not_configured — set supabase.jwks-uri + supabase.issuer-uri to enable Supabase auth");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // ── TEMP: correlationId in MDC for all auth-layer logs ────────────────
        String correlationId = resolveCorrelationId(request);
        MDC.put("correlationId", correlationId);

        boolean tenantBound = false;
        try {
            String authorization = request.getHeader(AUTHORIZATION_HEADER);
            boolean bearerPresent = authorization != null && authorization.startsWith(BEARER_PREFIX);

            log.info("AUTH_REQUEST_START method={} path={} bearerPresent={} correlationId={}",
                    request.getMethod(), request.getRequestURI(), bearerPresent, correlationId);

            if (!bearerPresent) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            log.debug("JWT_EXTRACTED path={} tokenLength={} correlationId={}",
                    request.getRequestURI(), token.length(), correlationId);

            boolean authenticated = tryPlatformAuth(token, request);
            if (!authenticated) {
                authenticated = trySupabaseAuth(token, request);
            }

            if (!authenticated) {
                log.warn("JWT_AUTH_FAILED path={} reason=token_not_accepted_by_any_decoder bearerPresent=true correlationId={}",
                        request.getRequestURI(), correlationId);
                // Intentionally fall through — Spring Security authorization rules own the 401.
            }

            UserPrincipal principal = resolveCurrentPrincipal();
            if (principal != null && principal.getTenantId() != null) {
                TenantContext.set(principal.getTenantId());
                tenantBound = true;
            }

            var finalAuth = SecurityContextHolder.getContext().getAuthentication();
            log.info("AUTH_CHAIN_PASS path={} authenticated={} userId={} authorities={}",
                    request.getRequestURI(),
                    finalAuth != null && finalAuth.isAuthenticated(),
                    principal != null ? principal.getUserId() : "none",
                    finalAuth != null ? finalAuth.getAuthorities() : "[]");

            filterChain.doFilter(request, response);
        } finally {
            if (tenantBound) {
                TenantContext.clear();
            }
            MDC.remove("correlationId");
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-ID");
        return (header != null && !header.isBlank()) ? header.trim() : UUID.randomUUID().toString();
    }

    // ── Platform JWT path (HS256, issued by this server) ─────────────────────

    private boolean tryPlatformAuth(String token, HttpServletRequest request) {
        try {
            log.debug("JWT_VALIDATION_START decoder=PLATFORM_HS256 path={}", request.getRequestURI());

            if (!jwtService.isTokenValid(token)) {
                log.debug("JWT_VALIDATION_FAILED decoder=PLATFORM_HS256 reason=invalid_token path={}",
                        request.getRequestURI());
                return false;
            }

            String tokenType = jwtService.extractTokenType(token);
            if (!"ACCESS".equals(tokenType)) {
                log.debug("JWT_VALIDATION_FAILED decoder=PLATFORM_HS256 reason=wrong_token_type type={} path={}",
                        tokenType, request.getRequestURI());
                return false;
            }

            UUID userId   = jwtService.extractUserIdAsUuid(token);
            UUID tenantId = jwtService.extractTenantIdAsUuid(token);
            List<String> roles = jwtService.extractRoles(token);

            UserPrincipal principal = UserPrincipal.builder()
                    .userId(userId)
                    .tenantId(tenantId)
                    .phone(jwtService.extractPhone(token))
                    .roles(roles)
                    .build();

            setAuthentication(principal, roles, request);

            log.info("JWT_VALIDATION_SUCCESS decoder=PLATFORM_HS256 userId={} tenantId={} path={} authorities={}",
                    userId, tenantId, request.getRequestURI(), buildAuthorities(roles));
            return true;

        } catch (Exception ex) {
            log.debug("JWT_VALIDATION_FAILED decoder=PLATFORM_HS256 type={} error={} path={}",
                    ex.getClass().getSimpleName(), ex.getMessage(), request.getRequestURI());
            return false;
        }
    }

    // ── Supabase JWT path (JWKS, asymmetric signature) ────────────────────────

    private boolean trySupabaseAuth(String token, HttpServletRequest request) {
        if (supabaseDecoder == null) {
            log.warn("JWT_VALIDATION_FAILED decoder=SUPABASE_JWKS reason=decoder_not_configured path={}",
                    request.getRequestURI());
            return false;
        }

        log.debug("JWT_VALIDATION_START decoder=SUPABASE_JWKS path={}", request.getRequestURI());

        Jwt jwt;
        try {
            // NimbusJwtDecoder.withJwkSetUri validates: signature, expiry, not-before,
            // issuer (via JwtIssuerValidator), and audience (via custom validator added below).
            jwt = supabaseDecoder.decode(token);
        } catch (Exception ex) {
            log.warn("JWT_VALIDATION_FAILED decoder=SUPABASE_JWKS type={} message={} path={}",
                    ex.getClass().getSimpleName(), ex.getMessage(), request.getRequestURI(), ex);
            return false;
        }

        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "unknown";
        String role   = jwt.getClaimAsString("role");
        String email  = jwt.getClaimAsString("email");
        String sub    = jwt.getSubject();
        List<String> aud = jwt.getAudience();
        String alg    = jwt.getHeaders().get("alg") != null ? jwt.getHeaders().get("alg").toString() : "unknown";

        log.info("JWT_VALIDATION_SUCCESS decoder=SUPABASE_JWKS alg={} iss={} aud={} role={} sub={} path={}",
                alg, issuer, aud, role, sub, request.getRequestURI());

        if (!"authenticated".equals(role)) {
            log.warn("JWT_AUTH_FAILED reason=role_not_authenticated role={} path={}", role, request.getRequestURI());
            return false;
        }

        if (email == null || email.isBlank()) {
            log.warn("JWT_AUTH_FAILED reason=email_claim_absent sub={} path={}", sub, request.getRequestURI());
            return false;
        }

        log.info("JWT_EMAIL_EXTRACTED email={} sub={} path={}", email, sub, request.getRequestURI());

        // Resolve the internal platform user, if one exists.
        var platformUser = userRepository.findByEmail(email).orElse(null);

        List<String> roles = List.of("authenticated");
        UserPrincipal principal;

        if (platformUser == null) {
            // External authentication succeeded; no platform user row yet.
            // Set a provisional principal (userId=null) so the request is authenticated from
            // Spring Security's perspective. POST /api/v1/users/provision uses email from the
            // request body and does not require a platform userId, so it succeeds here.
            // Any service that calls currentUserProvider.getCurrentUserId() will receive an
            // IllegalStateException("User not provisioned") rather than a silent 401.
            log.info("JWT_PLATFORM_USER_NOT_FOUND email={} path={} — userId=null provisional principal set — call /users/provision",
                    email, request.getRequestURI());
            principal = UserPrincipal.builder()
                    .userId(null)
                    .tenantId(null)
                    .phone(email)
                    .roles(roles)
                    .build();
        } else {
            log.info("JWT_PLATFORM_USER_FOUND email={} userId={} path={}", email, platformUser.getId(), request.getRequestURI());
            principal = UserPrincipal.builder()
                    .userId(platformUser.getId())
                    .tenantId(null)  // Citizens via Supabase carry no operator tenant context.
                    .phone(email)
                    .roles(roles)
                    .build();
        }

        setAuthentication(principal, roles, request);

        var currentAuth = SecurityContextHolder.getContext().getAuthentication();
        List<GrantedAuthority> authorities = buildAuthorities(roles);
        log.info("SECURITY_CONTEXT_SET type=SUPABASE provisioned={} userId={} authorities={} path={}",
                platformUser != null, platformUser != null ? platformUser.getId() : null,
                authorities, request.getRequestURI());
        log.info("SECURITY_CONTEXT_AUTHENTICATED path={} isAuthenticated={} principalClass={} userId={} authorities={}",
                request.getRequestURI(),
                currentAuth != null && currentAuth.isAuthenticated(),
                currentAuth != null ? currentAuth.getPrincipal().getClass().getSimpleName() : "null",
                principal.getUserId(),
                authorities);
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setAuthentication(UserPrincipal principal, List<String> roles, HttpServletRequest request) {
        List<GrantedAuthority> authorities = buildAuthorities(roles);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private List<GrantedAuthority> buildAuthorities(List<String> roles) {
        return roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private UserPrincipal resolveCurrentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        return null;
    }

    /**
     * Builds a JWKS-backed JwtDecoder with explicit algorithm constraints and claim validators.
     *
     * Algorithm selection:
     *   Explicitly declares ES256 (ECDSA) and RS256 (RSA) via jwsAlgorithms(). This is required
     *   because the no-argument build() form uses JWSAlgorithmFamilyJWSKeySelector which performs
     *   an eager JWKS fetch at startup and may fail or default to RSA-only if the fetch encounters
     *   any issue. By declaring algorithms explicitly, Spring Security uses JWSVerificationKeySelector
     *   instead — lazy JWKS fetch on first use, deterministic EC + RSA key matching.
     *
     *   Supabase projects issue ES256 (ECDSA P-256) signed tokens. RS256 is included for resilience
     *   against future algorithm rotation. Keys are fetched from the configured JWKS URI.
     *
     * Validators applied:
     *   - JwtTimestampValidator  — expiry + not-before
     *   - JwtIssuerValidator     — iss must equal supabase.issuer-uri
     *   - audience validator     — aud must contain "authenticated"
     */
    private static JwtDecoder buildSupabaseDecoder(String jwksUri, String issuerUri) {
        if (jwksUri == null || jwksUri.isBlank()) {
            return null;
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                .jwsAlgorithms(algorithms -> {
                    algorithms.add(SignatureAlgorithm.ES256);
                    algorithms.add(SignatureAlgorithm.RS256);
                })
                .build();

        OAuth2TokenValidator<Jwt> baseValidator = (issuerUri != null && !issuerUri.isBlank())
                ? JwtValidators.createDefaultWithIssuer(issuerUri)
                : JwtValidators.createDefault();

        // Supabase aud claim is the string "authenticated" (parsed as a single-element list).
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> aud = jwt.getAudience();
            if (aud != null && aud.contains("authenticated")) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_audience",
                    "Required audience 'authenticated' not present; got " + aud,
                    null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(baseValidator, audienceValidator));
        return decoder;
    }
}
