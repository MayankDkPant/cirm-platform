package com.cxp.platform.auth.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Issues and validates platform JWTs (HS256).
 *
 * governing_body_id (tenant) claim policy:
 *   - Included when the principal has a non-null tenantId (valid UUID string).
 *   - Omitted entirely when tenantId is null — never written as "UNASSIGNED"
 *     or any other non-UUID sentinel.
 * This prevents UUID parse failures in JwtAuthenticationFilter and makes
 * the absence of a tenant explicit and safely detectable.
 */
@Service
@Slf4j
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;

    public JwtService(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            @Value("${security.jwt.access-token-expiration-minutes:30}") long accessTokenExpirationMinutes,
            @Value("${security.jwt.refresh-token-expiration-days:30}") long refreshTokenExpirationDays) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    /**
     * Generates a short-lived ACCESS token.
     * governing_body_id is included only when tenantId is non-null.
     */
    public String generateAccessToken(UserPrincipal principal) {
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(principal.getUserId(), "principal.userId must not be null");

        List<String> roles = principal.getRoles() == null ? List.of() : principal.getRoles();
        Instant now    = Instant.now();
        Instant expiry = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .subject(principal.getUserId().toString())
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("phone", principal.getPhone())
                .claim("roles", roles)
                .claim("token_type", "ACCESS");

        if (principal.getTenantId() != null) {
            builder.claim("governing_body_id", principal.getTenantId().toString());
        }

        return jwtEncoder.encode(JwtEncoderParameters.from(builder.build())).getTokenValue();
    }

    /**
     * Generates a long-lived REFRESH token.
     * Same governing_body_id omission policy as generateAccessToken.
     */
    public String generateRefreshToken(UserPrincipal principal) {
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(principal.getUserId(), "principal.userId must not be null");

        List<String> roles = principal.getRoles() == null ? List.of() : principal.getRoles();
        Instant now    = Instant.now();
        Instant expiry = now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .subject(principal.getUserId().toString())
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("phone", principal.getPhone())
                .claim("roles", roles)
                .claim("token_type", "REFRESH");

        if (principal.getTenantId() != null) {
            builder.claim("governing_body_id", principal.getTenantId().toString());
        }

        return jwtEncoder.encode(JwtEncoderParameters.from(builder.build())).getTokenValue();
    }

    /** Validates token signature and expiration. */
    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (BadJwtException | IllegalArgumentException ex) {
            log.warn("jwt_validation_failed reason={}", ex.getMessage());
            return false;
        }
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractPhone(String token) {
        return parseClaims(token).getClaimAsString("phone");
    }

    public String extractTenantId(String token) {
        return parseClaims(token).getClaimAsString("governing_body_id");
    }

    public List<String> extractRoles(String token) {
        Object value = parseClaims(token).getClaims().get("roles");
        if (value == null) return List.of();
        if (value instanceof List<?> rawList) {
            List<String> roles = new ArrayList<>(rawList.size());
            for (Object role : rawList) {
                if (role != null) roles.add(role.toString());
            }
            return roles;
        }
        return List.of(value.toString());
    }

    public String extractTokenType(String token) {
        return parseClaims(token).getClaimAsString("token_type");
    }

    public long getAccessTokenExpirationMinutes() {
        return accessTokenExpirationMinutes;
    }

    public long getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }

    private Jwt parseClaims(String token) {
        return jwtDecoder.decode(token);
    }

    public UUID extractUserIdAsUuid(String token) {
        return UUID.fromString(extractUserId(token));
    }

    /**
     * Extracts the governing_body_id claim as a UUID.
     *
     * Returns null when the claim is absent (tenant-less tokens issued for citizen flows)
     * or when the value is blank. Logs a warning and returns null for values that are
     * present but not a valid UUID (e.g. legacy tokens containing "UNASSIGNED") so that
     * old tokens degrade gracefully rather than crashing the filter.
     */
    public UUID extractTenantIdAsUuid(String token) {
        String tenantId = extractTenantId(token);
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException ex) {
            log.warn("jwt_tenant_claim_not_uuid value='{}' — treating as absent", tenantId);
            return null;
        }
    }
}
