package com.cirm.platform.auth.infrastructure.persistence;

import com.cirm.platform.auth.domain.RefreshToken;
import com.cirm.platform.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

import java.util.Optional;

/**
 * Repository responsible for persistence operations on RefreshToken entities.
 *
 * Refresh tokens represent long-lived login sessions for mobile clients.
 * Tokens are stored as BCrypt hashes and never stored in raw form.
 *
 * This repository exposes query methods used by the authentication layer
 * to validate and revoke refresh tokens.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Fetches all active (non-revoked) refresh tokens.
     *
     * Why this exists:
     * When a refresh token is presented by a client, we cannot search
     * by token value because we store only a hash. Instead, we load
     * active tokens and compare hashes using BCrypt.
     *
     * This method is used during refresh token validation.
     */
    List<RefreshToken> findAllByRevokedFalse();

    /**
     * Fetches all active refresh tokens belonging to a specific user.
     *
     * Why this exists:
     * Used when we want to revoke all sessions for a user, such as:
     * - logout from all devices
     * - suspected account compromise
     * - security policy enforcement
     */
    List<RefreshToken> findAllByUserAndRevokedFalse(User user);

    Optional<RefreshToken> findByTokenIdAndRevokedFalse(UUID tokenId);
}