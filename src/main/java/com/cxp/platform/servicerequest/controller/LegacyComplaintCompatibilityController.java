package com.cxp.platform.servicerequest.controller;

import com.cxp.platform.servicerequest.dto.LegacyComplaintCreateRequest;
import com.cxp.platform.servicerequest.dto.ServiceRequestCreateRequest;
import com.cxp.platform.servicerequest.dto.ServiceRequestResponse;
import com.cxp.platform.servicerequest.entity.ServiceRequestType;
import com.cxp.platform.servicerequest.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class LegacyComplaintCompatibilityController {

    private final ServiceRequestService serviceRequestService;

    @PostMapping
    public ServiceRequestResponse createComplaint(@Valid @RequestBody LegacyComplaintCreateRequest request) {
        return serviceRequestService.create(
                new ServiceRequestCreateRequest(
                        ServiceRequestType.COMPLAINT,
                        request.title(),
                        request.description(),
                        request.category(),
                        request.priority(),
                        firstPresent(request.addressText(), request.reportedLocationText()),
                        request.latitude(),
                        request.longitude(),
                        request.departmentId(),
                        request.aiConversationId()
                )
        );
    }

    private String firstPresent(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }
}
