package com.cxp.platform.ai.application;

import com.cxp.platform.ai.port.AiClassificationResponse;
import com.cxp.platform.ai.port.AiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiClassificationService {

    private final AiClient aiClient;

    public AiClassificationResponse classifyServiceRequest(String requestText) {
        return aiClient.classify(requestText);
    }
}
