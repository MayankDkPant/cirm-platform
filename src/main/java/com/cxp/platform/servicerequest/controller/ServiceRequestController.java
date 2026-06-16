package com.cxp.platform.servicerequest.controller;

import com.cxp.platform.api.error.ApiError;
import com.cxp.platform.common.openapi.PagedServiceRequestResponse;
import com.cxp.platform.common.openapi.StandardApiErrors;
import com.cxp.platform.servicerequest.dto.AiServiceRequestAnalyzeRequest;
import com.cxp.platform.servicerequest.dto.AiServiceRequestAnalyzeResponse;
import com.cxp.platform.servicerequest.dto.ServiceRequestCreateRequest;
import com.cxp.platform.servicerequest.dto.ServiceRequestEventsResponse;
import com.cxp.platform.servicerequest.dto.ServiceRequestResponse;
import com.cxp.platform.servicerequest.dto.ServiceRequestStatusUpdateRequest;
import com.cxp.platform.servicerequest.service.AiServiceRequestAnalyzeService;
import com.cxp.platform.servicerequest.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service request endpoints.
 *
 * Citizen-safe endpoints (no TenantContext required — work without a governing body):
 *   POST   /analyze      — AI enrichment preview, no persistence
 *   POST   /             — create a new service request (ward derived from GPS)
 *   GET    /             — list the authenticated citizen's own submitted requests
 *   GET    /{id}         — retrieve a specific request (citizen sees own; operator sees tenant)
 *
 * Operator-only endpoints (require TenantContext from a platform JWT):
 *   PATCH  /{id}/status  — status lifecycle management
 */
@Tag(name = "Service Requests")
@RestController
@RequestMapping("/api/v1/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final AiServiceRequestAnalyzeService analyzeService;

    @Operation(
            summary     = "Preview AI enrichment for a service request",
            description = "Runs AI classification on the supplied title, description, and GPS coordinates. " +
                          "Returns a suggested category, priority, and routing hint — no data is persisted. " +
                          "Intended for pre-submit UX enrichment; the result is advisory, not authoritative."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "AI enrichment preview returned")
    @ApiResponse(responseCode = "400",
            description = "Validation failure — required fields missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/analyze")
    public AiServiceRequestAnalyzeResponse analyze(@Valid @RequestBody AiServiceRequestAnalyzeRequest request) {
        return analyzeService.analyze(request.title(), request.description(), request.latitude(), request.longitude());
    }

    @Operation(
            summary     = "Submit a civic service request",
            description = "Creates a new service request scoped to the authenticated citizen. " +
                          "Ward routing is derived server-side from the supplied GPS coordinates — " +
                          "no ward selection is required from the client. " +
                          "Supply an Idempotency-Key header to prevent duplicate submissions on retry."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200",
            description = "Service request created, or cached response returned for a duplicate Idempotency-Key.")
    @ApiResponse(responseCode = "400",
            description = "Validation failure — required fields missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422",
            description = "Cannot assign ward jurisdiction — no GPS coordinates supplied and no ward in the " +
                          "citizen's profile. Complete onboarding (POST /api/v1/users/provision) or supply " +
                          "latitude/longitude in the request body.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ServiceRequestResponse create(
            @Valid @RequestBody ServiceRequestCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return serviceRequestService.create(request, idempotencyKey);
    }

    @Operation(
            summary     = "List the authenticated citizen's service requests",
            description = "Returns a paginated list of service requests submitted by the caller. " +
                          "Citizens see only their own requests. " +
                          "Default page size is 20; use ?page and ?size to paginate. " +
                          "Pagination envelope: 'page' is the canonical 0-based page index. " +
                          "'number' is a deprecated backward-compatibility alias for 'page' — " +
                          "SDK consumers must migrate to 'page'; 'number' will be removed in a future release."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Paginated list of service requests",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PagedServiceRequestResponse.class)))
    @GetMapping
    public Page<ServiceRequestResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return serviceRequestService.list(PageRequest.of(page, size));
    }

    @Operation(
            summary     = "Retrieve a service request",
            description = "Returns a single service request by ID. " +
                          "Citizens can retrieve only their own requests. " +
                          "Operators with a valid platform JWT can retrieve any request within their tenant."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Service request returned")
    @ApiResponse(responseCode = "404",
            description = "Service request not found for the given ID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    public ServiceRequestResponse get(@PathVariable UUID id) {
        return serviceRequestService.get(id);
    }

    @Operation(
            summary     = "List the event history of a service request",
            description = "Returns the immutable ordered event log for the given service request — " +
                          "status transitions, AI classifications, and external sync events. " +
                          "Events are append-only and never modified after creation."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Ordered event log returned")
    @ApiResponse(responseCode = "404",
            description = "Service request not found for the given ID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}/events")
    public ServiceRequestEventsResponse getEvents(@PathVariable UUID id) {
        return serviceRequestService.getEvents(id);
    }

    @Operation(
            summary     = "Update service request status",
            description = "Advances the lifecycle state of a service request. " +
                          "Operator-only — requires the OPERATOR role on the platform JWT. " +
                          "Each transition is recorded as an immutable event in the request's event log."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Service request status updated")
    @ApiResponse(responseCode = "400",
            description = "Validation failure or invalid status transition.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403",
            description = "Forbidden — the caller does not have the OPERATOR role.",
            content = @Content)
    @ApiResponse(responseCode = "404",
            description = "Service request not found for the given ID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OPERATOR')")
    public ServiceRequestResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ServiceRequestStatusUpdateRequest request) {
        return serviceRequestService.updateStatus(id, request.status());
    }
}
