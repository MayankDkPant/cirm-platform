package com.cxp.platform.servicerequest.service;

import com.cxp.platform.servicerequest.entity.ServiceRequest;
import com.cxp.platform.servicerequest.entity.ServiceRequestEvent;
import com.cxp.platform.servicerequest.entity.ServiceRequestStatus;
import com.cxp.platform.servicerequest.repository.ServiceRequestEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Append-only writer for ServiceRequestEvent audit records.
 *
 * Idempotency contract:
 *   recordCreated     — called once per service request, inside the same @Transactional
 *                       as the service_request INSERT. Cannot produce a duplicate CREATED event
 *                       unless the outer transaction itself retries (idempotency_key seals that).
 *   recordStatusChanged — operator-triggered, serialized through the status update transaction.
 *                         Not retry-sensitive.
 *   recordSyncSuccess — called exactly once per successful Salesforce push; the service request
 *                       moves to SYNCED immediately, removing it from future sync polls.
 *   recordSyncFailure — appended on every retry attempt (up to MAX_ATTEMPTS=3). Multiple
 *                       SYNC_FAILED events per request are EXPECTED — they record each attempt.
 *
 * Known distributed-scaling gap: if two app instances pick up the same PENDING request
 * simultaneously, both may push to Salesforce and append events. Mitigation requires
 * optimistic locking or SELECT FOR UPDATE on the sync poll — deferred to the horizontal-
 * scaling phase. Acceptable for single-instance deployments.
 */
@Service
@RequiredArgsConstructor
public class ServiceRequestEventService {

    private final ServiceRequestEventRepository eventRepository;

    public void recordCreated(ServiceRequest request, String enrichmentMetadataJson) {
        eventRepository.save(ServiceRequestEvent.builder()
                .serviceRequestId(request.getId())
                .eventType("CREATED")
                .newStatus(request.getStatus().name())
                .actorType("USER")
                .actorUserId(request.getCreatedByUserId())
                .metadataJson(enrichmentMetadataJson)
                .build());
    }

    public void recordStatusChanged(ServiceRequest request, ServiceRequestStatus oldStatus, UUID actorUserId) {
        // actorType=OPERATOR: status changes are operator-only actions (enforced by @PreAuthorize in the controller).
        eventRepository.save(ServiceRequestEvent.builder()
                .serviceRequestId(request.getId())
                .eventType("STATUS_CHANGED")
                .oldStatus(oldStatus.name())
                .newStatus(request.getStatus().name())
                .actorType("OPERATOR")
                .actorUserId(actorUserId)
                .build());
    }

    public void recordSyncSuccess(UUID serviceRequestId, String externalReference) {
        eventRepository.save(ServiceRequestEvent.builder()
                .serviceRequestId(serviceRequestId)
                .eventType("SYNCED_TO_SALESFORCE")
                .notes(externalReference)
                .build());
    }

    public void recordSyncFailure(UUID serviceRequestId, String error) {
        eventRepository.save(ServiceRequestEvent.builder()
                .serviceRequestId(serviceRequestId)
                .eventType("SYNC_FAILED")
                .notes(error)
                .build());
    }

    public List<ServiceRequestEvent> getEventsForRequest(UUID serviceRequestId) {
        return eventRepository.findByServiceRequestIdOrderByCreatedAtAsc(serviceRequestId);
    }
}
