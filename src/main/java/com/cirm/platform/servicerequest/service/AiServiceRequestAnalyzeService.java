package com.cirm.platform.servicerequest.service;

import com.cirm.platform.ai.application.AiClassificationService;
import com.cirm.platform.ai.port.AiClassificationResponse;
import com.cirm.platform.servicerequest.dto.AiServiceRequestAnalyzeResponse;
import com.cirm.platform.servicerequest.entity.ServiceRequestType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiServiceRequestAnalyzeService {

    private final AiClassificationService aiClassificationService;

    public AiServiceRequestAnalyzeResponse analyze(String text) {
        AiClassificationResponse classification = aiClassificationService.classifyServiceRequest(text);

        return new AiServiceRequestAnalyzeResponse(
                resolveType(classification.getType()),
                classification.getCategory(),
                classification.getPriority(),
                classification.getConfidence()
        );
    }

    private ServiceRequestType resolveType(String intent) {
        if (intent == null || intent.isBlank()) {
            return ServiceRequestType.COMPLAINT;
        }

        try {
            return ServiceRequestType.valueOf(intent.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ServiceRequestType.COMPLAINT;
        }
    }
}
