package com.cxp.platform.auth.application;

import com.cxp.platform.auth.domain.RefreshToken;
import com.cxp.platform.auth.domain.User;
import com.cxp.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Enterprise refresh token service implementing:
 * - Hashed token storage
 * - TokenId fast lookup
 * - Refresh token rotation
 * - Token revocation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Creates a new refresh token and stores its hash in DB.
     * Returns token in format: tokenId.rawJwt
     */
    public String createRefreshToken(UserPrincipal principal, User user) {

        String rawJwt = jwtService.generateRefreshToken(principal);
        UUID tokenId = UUID.randomUUID();

        String tokenHash = encoder.encode(rawJwt);

        RefreshToken entity = RefreshToken.builder()
                .tokenId(tokenId)
                .tokenHash(tokenHash)
                .user(user)
                .expiryDate(Instant.now()
                        .plus(jwtService.getRefreshTokenExpirationDays(), ChronoUnit.DAYS))
                .revoked(false)
                .build();

        repository.save(entity);

        return tokenId + "." + rawJwt;
    }

    /**
     * Validates refresh token and returns stored entity.
     * Token format: tokenId.rawJwt
     */
    public Optional<RefreshToken> validateRefreshToken(String refreshToken) {

        try {
            String[] parts = refreshToken.split("\\.");
            UUID tokenId = UUID.fromString(parts[0]);
            String rawJwt = parts[1];

            Optional<RefreshToken> tokenOpt =
                    repository.findByTokenIdAndRevokedFalse(tokenId);

            if (tokenOpt.isEmpty()) return Optional.empty();

            RefreshToken token = tokenOpt.get();

            if (token.getExpiryDate().isBefore(Instant.now())) return Optional.empty();

            if (!encoder.matches(rawJwt, token.getTokenHash())) return Optional.empty();

            return Optional.of(token);

        } catch (Exception ex) {
            log.warn("invalid_refresh_token");
            return Optional.empty();
        }
    }

    /**
     * Revokes token after use (rotation).
     */
    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        repository.save(token);
    }
}