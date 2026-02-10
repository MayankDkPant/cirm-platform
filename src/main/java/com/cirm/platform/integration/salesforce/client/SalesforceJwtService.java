package com.cirm.platform.integration.salesforce.client;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class SalesforceJwtService {

    @Value("${salesforce.client-id}")
    private String clientId;

    @Value("${salesforce.username}")
    private String username;

    @Value("${salesforce.login-url}")
    private String loginUrl;

    @Value("${salesforce.private-key-path}")
    private String privateKeyPath;

    public String generateJwt() {
        try {
            PrivateKey privateKey = loadPrivateKey();
            JWSSigner signer = new RSASSASigner(privateKey);

            Instant now = Instant.now();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(clientId)
                    .subject(username)
                    .audience(loginUrl)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(300)))
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.RS256),
                    claimsSet
            );

            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Salesforce JWT", e);
        }
    }

    private PrivateKey loadPrivateKey() throws Exception {

    InputStream is = getClass()
            .getClassLoader()
            .getResourceAsStream("keys/salesforce.key");

    if (is == null) {
        throw new RuntimeException("Private key not found in classpath");
    }

    String key = new String(is.readAllBytes())
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

    byte[] decoded = Base64.getDecoder().decode(key);

    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return keyFactory.generatePrivate(spec);
}

}
