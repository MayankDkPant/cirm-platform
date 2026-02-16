package com.cirm.platform.auth.service;

import com.cirm.platform.auth.domain.RefreshToken;
import com.cirm.platform.auth.domain.User;
import com.cirm.platform.auth.infrastructure.persistence.RefreshTokenRepository;
import com.cirm.platform.auth.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Profile("auth")
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtEncoder jwtEncoder;
    private final PasswordEncoder passwordEncoder;

    private static final long ACCESS_TOKEN_MINUTES = 15;
    private static final long REFRESH_TOKEN_DAYS = 7;

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

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cirm-platform")
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_MINUTES, ChronoUnit.MINUTES))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String generateAndStoreRefreshToken(User user) {

        String rawToken = UUID.randomUUID().toString();
        String hash = passwordEncoder.encode(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hash)
                .user(user)
                .expiryDate(Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    public record AuthResponse(String accessToken, String refreshToken) {}
}
