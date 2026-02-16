package com.cirm.platform.ai.client;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client responsible for communicating with the external Python AI service.
 *
 * This class acts as an infrastructure adapter between the Java backend
 * and the AI microservice running in Docker.
 *
 * Responsibilities:
 * - Send requests to AI service
 * - Handle failures and provide fallback response
 *
 * This class MUST NOT contain business logic.
 */
@Component
public class AiClient {

    private final RestTemplate restTemplate;

    public AiClient() {
        this.restTemplate = new RestTemplate();
    }

    public AiClassificationResponse classify(String complaintText) {

        String url = "http://host.docker.internal:8000/classify";

        Map<String, String> requestBody = Map.of(
                "text", complaintText
        );

        try {
            ResponseEntity<AiClassificationResponse> response =
                    restTemplate.postForEntity(
                            url,
                            requestBody,
                            AiClassificationResponse.class
                    );

            return response.getBody();

        } catch (Exception e) {
            // AI fallback response when service is unavailable
            AiClassificationResponse fallback = new AiClassificationResponse();
            fallback.setIntent("COMPLAINT");
            fallback.setDepartment("OTHER");
            fallback.setPriority("MEDIUM");
            fallback.setConfidence(0.5);
            return fallback;
        }
    }
}
