package com.cxp.platform.auth.application;

import com.cxp.platform.auth.domain.RefreshToken;
import com.cxp.platform.auth.domain.User;
import com.cxp.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import com.cxp.platform.auth.infrastructure.persistence.UserRepository;
import com.cxp.platform.governance.domain.GoverningBody;
import com.cxp.platform.governance.repository.GoverningBodyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoverningBodyRepository governingBodyRepository;

    public AuthResponse mockLogin(String email) {

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email));

        String accessToken = generateAccessToken(user);
        String refreshToken = generateAndStoreRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    private User createUser(String email) {
        User user = User.builder()
                .email(email)
                .role(User.Role.CITIZEN)
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    private String generateAccessToken(User user) {
        UUID tenantId = governingBodyRepository.findFirstByIsActiveTrue()
                .map(GoverningBody::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "No active governing_body found. Run Flyway migrations and seed data first."));

        UserPrincipal principal = UserPrincipal.builder()
                .userId(user.getId())
                .phone(user.getEmail())
                .tenantId(tenantId)
                .roles(List.of(user.getRole().name()))
                .build();
        return jwtService.generateAccessToken(principal);
    }

    private String generateAndStoreRefreshToken(User user) {

        String rawToken = UUID.randomUUID().toString();
        String hash = passwordEncoder.encode(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hash)
                .user(user)
                .expiryDate(Instant.now().plus(jwtService.getRefreshTokenExpirationDays(), ChronoUnit.DAYS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    public record AuthResponse(String accessToken, String refreshToken) {}
}
