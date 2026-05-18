package com.cirm.platform.auth.web.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Security key configuration for platform-issued HS256 JWT tokens.
 *
 * This configuration exposes JWT encoder/decoder beans backed by a shared
 * HMAC secret so access tokens can be issued and validated consistently
 * across the application.
 */
@Configuration
public class JwtConfig {

    /**
     * Creates the HMAC key used for HS256 signing and validation.
     *
     * @param base64Secret base64-encoded secret from configuration
     * @return initialized HMAC SHA-256 secret key
     */
    @Bean
    public SecretKey jwtSecretKey(@Value("${security.jwt.secret}") String base64Secret) {
        byte[] decoded = Base64.getDecoder().decode(base64Secret);
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    /**
     * Creates JWT encoder for issuing access tokens.
     *
     * @param secretKey HMAC key
     * @return JWT encoder
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    /**
     * Creates JWT decoder for validating bearer tokens.
     *
     * @param secretKey HMAC key
     * @return JWT decoder configured for HS256
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Provides password encoder for hashing refresh tokens and credentials.
     *
     * @return BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
