package com.cirm.platform.ai;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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

            // Fallback if AI fails
            AiClassificationResponse fallback = new AiClassificationResponse();
            fallback.setIntent("COMPLAINT");
            fallback.setDepartment("OTHER");
            fallback.setPriority("MEDIUM");
            fallback.setConfidence(0.5);

            return fallback;
        }
    }
}
