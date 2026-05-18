package com.cirm.platform.auth.application;

import com.cirm.platform.auth.domain.RefreshToken;
import com.cirm.platform.auth.domain.User;
import com.cirm.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import com.cirm.platform.auth.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/**
 * Application service orchestrating use cases for the module.
 */
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    private static final UUID DEFAULT_GOVERNING_BODY_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

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
        UserPrincipal principal = UserPrincipal.builder()
                .userId(user.getId())
                .phone(user.getEmail())
                .tenantId(DEFAULT_GOVERNING_BODY_ID)
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
