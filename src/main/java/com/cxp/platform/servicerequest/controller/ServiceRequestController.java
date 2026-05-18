package com.cxp.platform.servicerequest.controller;

import com.cxp.platform.servicerequest.dto.ServiceRequestCreateRequest;
import com.cxp.platform.servicerequest.dto.ServiceRequestResponse;
import com.cxp.platform.servicerequest.dto.ServiceRequestStatusUpdateRequest;
import com.cxp.platform.servicerequest.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @PostMapping
    public ServiceRequestResponse create(@Valid @RequestBody ServiceRequestCreateRequest request) {
        return serviceRequestService.create(request);
    }

    @GetMapping
    public List<ServiceRequestResponse> list() {
        return serviceRequestService.list();
    }

    @GetMapping("/{id}")
    public ServiceRequestResponse get(@PathVariable UUID id) {
        return serviceRequestService.get(id);
    }

    @PatchMapping("/{id}/status")
    public ServiceRequestResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ServiceRequestStatusUpdateRequest request) {
        return serviceRequestService.updateStatus(id, request.status());
    }
}
