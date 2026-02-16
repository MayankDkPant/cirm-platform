package com.cirm.platform.auth.infrastructure.persistence;

import com.cirm.platform.auth.domain.RefreshToken;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

@Profile("auth")
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
}
