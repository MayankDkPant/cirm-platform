package com.cirm.platform.integration.salesforce.client;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class SalesforceWebClientConfig {

    private final SalesforceTokenService tokenService;

    @Bean
    public WebClient salesforceWebClient() {

        return WebClient.builder()
                .filter(bearerAuthFilter())
                .build();
    }

    private ExchangeFilterFunction bearerAuthFilter() {

        return (request, next) -> {

            String token = tokenService.getValidToken();
            String instanceUrl = tokenService.getInstanceUrl();

            ClientRequest newRequest = ClientRequest.from(request)
                    .url(request.url().toString().startsWith("http")
                            ? request.url()
                            : java.net.URI.create(instanceUrl + request.url().toString()))
                    .headers(headers -> headers.setBearerAuth(token))
                    .build();

            return next.exchange(newRequest);
        };
    }
}
