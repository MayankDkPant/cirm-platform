package com.cxp.platform.servicerequest.dto;

import com.cxp.platform.servicerequest.entity.ServiceRequestType;

public record AiServiceRequestAnalyzeResponse(
        ServiceRequestType type,
        String category,
        String priority,
        double confidence
) {
}
