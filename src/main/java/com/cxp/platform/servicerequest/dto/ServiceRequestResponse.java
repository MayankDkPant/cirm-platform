package com.cxp.platform.servicerequest.dto;

import com.cxp.platform.servicerequest.entity.ServiceRequestStatus;
import com.cxp.platform.servicerequest.entity.ServiceRequestType;

import java.time.Instant;
import java.util.UUID;

public record ServiceRequestResponse(
        UUID id,
        ServiceRequestType type,
        String title,
        String description,
        String category,
        String priority,
        ServiceRequestStatus status,
        String addressText,
        Double latitude,
        Double longitude,
        UUID departmentId,
        UUID wardId,
        String wardName,
        Instant createdAt
) {
}
