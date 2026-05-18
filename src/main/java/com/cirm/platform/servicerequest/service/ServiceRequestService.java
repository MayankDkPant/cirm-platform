package com.cirm.platform.servicerequest.service;

import com.cirm.platform.common.tenant.TenantContext;
import com.cirm.platform.location.application.LocationIntelligenceService;
import com.cirm.platform.location.domain.LocationRoutingRequest;
import com.cirm.platform.location.domain.LocationRoutingResult;
import com.cirm.platform.servicerequest.dto.ServiceRequestCreateRequest;
import com.cirm.platform.servicerequest.dto.ServiceRequestResponse;
import com.cirm.platform.servicerequest.entity.ServiceRequest;
import com.cirm.platform.servicerequest.entity.ServiceRequestSource;
import com.cirm.platform.servicerequest.entity.ServiceRequestStatus;
import com.cirm.platform.servicerequest.exception.ServiceRequestNotFoundException;
import com.cirm.platform.servicerequest.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final LocationIntelligenceService locationIntelligenceService;
    private final ServiceRequestMapper mapper;

    @Transactional
    public ServiceRequestResponse create(ServiceRequestCreateRequest request) {
        UUID governingBodyId = TenantContext.get();
        LocationRoutingResult routing = resolveRouting(request);

        ServiceRequest serviceRequest = ServiceRequest.builder()
                .type(request.type())
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .priority(request.priority())
                .status(ServiceRequestStatus.OPEN)
                .addressText(resolveAddressText(request, routing))
                .latitude(request.latitude())
                .longitude(request.longitude())
                .departmentId(request.departmentId())
                .governingBodyId(governingBodyId)
                .wardId(routing.getWardId())
                .wardName(routing.getWardName())
                .formattedAddress(routing.getFormattedAddress())
                .aiConversationId(request.aiConversationId())
                .source(request.aiConversationId() == null
                        ? ServiceRequestSource.MANUAL
                        : ServiceRequestSource.AI_ASSISTED)
                .build();

        return mapper.toResponse(serviceRequestRepository.save(serviceRequest));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> list() {
        return serviceRequestRepository
                .findByGoverningBodyIdOrderByCreatedAtDesc(TenantContext.get())
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse get(UUID id) {
        return mapper.toResponse(findTenantScoped(id));
    }

    @Transactional
    public ServiceRequestResponse updateStatus(UUID id, ServiceRequestStatus status) {
        ServiceRequest request = findTenantScoped(id);
        request.setStatus(status);

        if (status == ServiceRequestStatus.RESOLVED || status == ServiceRequestStatus.CLOSED) {
            request.setResolvedAt(Instant.now());
        }

        return mapper.toResponse(serviceRequestRepository.save(request));
    }

    private ServiceRequest findTenantScoped(UUID id) {
        return serviceRequestRepository.findByIdAndGoverningBodyId(id, TenantContext.get())
                .orElseThrow(() -> new ServiceRequestNotFoundException(id));
    }

    private LocationRoutingResult resolveRouting(ServiceRequestCreateRequest request) {
        return locationIntelligenceService.resolveRouting(
                LocationRoutingRequest.builder()
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .reportedLocationText(request.addressText())
                        .build()
        );
    }

    private String resolveAddressText(ServiceRequestCreateRequest request, LocationRoutingResult routing) {
        if (request.addressText() != null && !request.addressText().isBlank()) {
            return request.addressText();
        }
        return routing.getFormattedAddress();
    }
}
