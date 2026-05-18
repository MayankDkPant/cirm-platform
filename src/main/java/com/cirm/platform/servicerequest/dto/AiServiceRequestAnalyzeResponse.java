package com.cirm.platform.servicerequest.dto;

import com.cirm.platform.servicerequest.entity.ServiceRequestType;

public record AiServiceRequestAnalyzeResponse(
        ServiceRequestType type,
        String category,
        String priority,
        double confidence
) {
}
