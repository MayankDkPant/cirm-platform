package com.cxp.platform.servicerequest.service;

import com.cxp.platform.ai.application.AiClassificationService;
import com.cxp.platform.ai.port.AiClassificationResponse;
import com.cxp.platform.integration.wardlocator.WardLocatorClient;
import com.cxp.platform.integration.wardlocator.WardLocatorResponse;
import com.cxp.platform.servicerequest.dto.AiServiceRequestAnalyzeResponse;
import com.cxp.platform.servicerequest.dto.AiServiceRequestAnalyzeResponse.WardLocationContext;
import com.cxp.platform.servicerequest.entity.ServiceRequestCategory;
import com.cxp.platform.servicerequest.entity.ServiceRequestPriority;
import com.cxp.platform.servicerequest.entity.ServiceRequestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceRequestAnalyzeService {

    private final AiClassificationService aiClassificationService;
    private final WardLocatorClient wardLocatorClient;

    /**
     * Advisory enrichment only — no persistence.
     *
     * Combines AI semantic classification with geographic ward resolution so the
     * citizen can review both before the authoritative create call.
     */
    public AiServiceRequestAnalyzeResponse analyze(
            String title, String description, Double latitude, Double longitude) {

        String text = (title != null && !title.isBlank())
                ? title + ". " + description
                : description;

        AiClassificationResponse classification = aiClassificationService.classifyServiceRequest(text);

        WardLocationContext location = resolveWardContext(latitude, longitude);

        return new AiServiceRequestAnalyzeResponse(
                resolveType(classification.getType()),
                resolveCategory(classification.getCategory()),
                resolvePriority(classification.getPriority()),
                classification.getConfidence(),
                classification.getCategory(),  // suggestedDepartment — raw AI string for transparency
                location
        );
    }

    private WardLocationContext resolveWardContext(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        Optional<WardLocatorResponse> ward = wardLocatorClient.locate(latitude, longitude);
        if (ward.isEmpty()) {
            log.warn("analyze_ward_unresolved lat={} lon={}", latitude, longitude);
            return null;
        }
        return new WardLocationContext(ward.get().wardNo(), ward.get().wardName());
    }

    /**
     * Normalises a raw AI category string to the canonical enum.
     * Unknown or null strings → OTHER. Never throws.
     */
    public static ServiceRequestCategory resolveCategory(String category) {
        if (category == null || category.isBlank()) {
            return ServiceRequestCategory.OTHER;
        }
        try {
            return ServiceRequestCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ServiceRequestCategory.OTHER;
        }
    }

    public static ServiceRequestType resolveType(String intent) {
        if (intent == null || intent.isBlank()) {
            return ServiceRequestType.COMPLAINT;
        }
        try {
            return ServiceRequestType.valueOf(intent.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ServiceRequestType.COMPLAINT;
        }
    }

    public static ServiceRequestPriority resolvePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return ServiceRequestPriority.MEDIUM;
        }
        try {
            return ServiceRequestPriority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ServiceRequestPriority.MEDIUM;
        }
    }
}

