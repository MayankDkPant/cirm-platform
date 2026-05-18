package com.cirm.platform.servicerequest.service;

import com.cirm.platform.servicerequest.dto.ServiceRequestResponse;
import com.cirm.platform.servicerequest.entity.ServiceRequest;
import org.springframework.stereotype.Component;

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
                request.getCreatedAt()
        );
    }
}
