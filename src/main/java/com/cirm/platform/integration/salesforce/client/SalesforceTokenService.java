package com.cirm.platform.integration.salesforce.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SalesforceTokenService {

    private final SalesforceJwtService jwtService;

    @Value("${salesforce.login-url}")
    private String loginUrl;

    private final WebClient.Builder webClientBuilder = WebClient.builder();

    private volatile String cachedToken;
    private volatile Instant expiryTime;
    private volatile String instanceUrl;

    public synchronized String getValidToken() {

        if (cachedToken == null ||
            expiryTime == null ||
            Instant.now().isAfter(expiryTime.minusSeconds(30))) {

            refreshToken();
        }

        return cachedToken;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    private void refreshToken() {

        String jwt = jwtService.generateJwt();

        TokenResponse response = webClientBuilder.build()
                .post()
                .uri(loginUrl + "/services/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type",
                                "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        .with("assertion", jwt))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (response == null || response.accessToken() == null) {
            throw new RuntimeException("Failed to obtain Salesforce access token");
        }

        this.cachedToken = response.accessToken();
        this.instanceUrl = response.instanceUrl();
        this.expiryTime = Instant.now().plusSeconds(300);
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("instance_url") String instanceUrl
    ) {}
}
