package com.cxp.platform.servicerequest.service;

import com.cxp.platform.servicerequest.dto.ServiceRequestResponse;
import com.cxp.platform.servicerequest.entity.ServiceRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ServiceRequestMapper {

    ServiceRequestResponse toResponse(ServiceRequest request) {
        return new ServiceRequestResponse(
                request.getId(),
                request.getType(),
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getPriority(),
                request.getStatus(),
                request.getAddressText(),
                request.getLatitude(),
                request.getLongitude(),
                request.getDepartmentId(),
                request.getWardId(),
                request.getWardName(),
                request.getAiConfidence(),
                request.isPublic(),
                request.getVisibilityScope(),
                request.getExternalSyncStatus(),
                request.getCreatedAt(),
                // contract fields
                null,                       // referenceNumber — assigned by Salesforce sync
                null,                       // department — deferred; see ServiceRequestResponse Javadoc
                0,                          // mediaCount — upload not yet implemented
                request.getUpdatedAt(),
                request.getResolvedAt(),
                List.of()                   // media — MUST be [] not null
        );
    }
}
