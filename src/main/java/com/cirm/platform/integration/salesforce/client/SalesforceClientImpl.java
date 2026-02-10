package com.cirm.platform.integration.salesforce.client;

import com.cirm.platform.integration.salesforce.dto.CreateCaseSfRequest;
import com.cirm.platform.integration.salesforce.dto.UpdateCaseSfRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class SalesforceClientImpl implements SalesforceClient {

    private final WebClient webClient;
    private final SalesforceTokenService tokenService;

    @Value("${salesforce.login-url}")
    private String loginUrl;

    @Override
    public String createCase(CreateCaseSfRequest request) {

        String accessToken = tokenService.getValidToken();

        CaseResponse response = webClient.post()
                .uri(tokenService.getInstanceUrl() + "/services/data/v60.0/sobjects/Case")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CaseResponse.class)
                .block();

        if (response == null || !response.success()) {
            throw new RuntimeException("Failed to create Salesforce Case");
        }

        return response.id();
    }

    @Override
    public void updateCase(String caseId, UpdateCaseSfRequest request) {

        String accessToken = tokenService.getValidToken();

        webClient.patch()
                .uri(tokenService.getInstanceUrl()  + "/services/data/v60.0/sobjects/Case/" + caseId)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    // Internal response mapping
    private record CaseResponse(
            @JsonProperty("id") String id,
            @JsonProperty("success") boolean success
    ) {}
}
