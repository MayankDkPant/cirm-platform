package com.cirm.platform.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a refresh token session.
 *
 * Refresh tokens are long-lived credentials issued to mobile clients.
 * The raw token is NEVER stored in the database.
 *
 * Instead:
 *  - tokenId     → public identifier used for fast DB lookup
 *  - tokenHash   → BCrypt hash of the actual refresh token
 *
 * This hybrid design ensures:
 *  - Secure storage
 *  - Scalable lookup
 *  - Support for token revocation
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    /**
     * Internal primary key.
     */
    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Public identifier of the refresh token.
     *
     * This value is sent to the client as part of the refresh token.
     * It is indexed and used for fast database lookup.
     */
    @Column(nullable = false, unique = true)
    private UUID tokenId;

    /**
     * BCrypt hash of the actual refresh token value.
     *
     * Raw token is never persisted.
     */
    @Column(nullable = false)
    private String tokenHash;

    /**
     * User who owns this refresh token.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Expiration timestamp of this refresh token.
     */
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * Indicates whether this token has been revoked.
     */
    @Column(nullable = false)
    private boolean revoked;

    /**
     * Timestamp when token was created.
     */
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.revoked = false;
    }
}