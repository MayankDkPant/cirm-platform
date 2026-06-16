package com.cxp.platform.integration.salesforce.application;

import com.cxp.platform.servicerequest.entity.ServiceRequest;
import com.cxp.platform.servicerequest.entity.ServiceRequestExternalSyncStatus;
import com.cxp.platform.servicerequest.repository.ServiceRequestRepository;
import com.cxp.platform.servicerequest.service.ServiceRequestEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Atomic write-back for Salesforce sync results.
 *
 * CONSISTENCY INVARIANT: the audit event (ServiceRequestEvent) and the status
 * update on ServiceRequest MUST be committed together. Without atomicity, a crash
 * between the two writes produces an inconsistent state:
 *   - event appended but status still PENDING/RETRYING → next poll re-syncs → duplicate Salesforce case
 *   - status updated but no event → silent sync with no audit trail
 *
 * This service is a separate Spring bean specifically because @Transactional requires
 * a Spring proxy — methods on the same bean as the @Scheduled worker are called
 * directly and bypass the proxy, so @Transactional would be silently ignored there.
 *
 * Consistency model: ATOMIC (both writes succeed or both roll back).
 * Scope: called once per service request per sync cycle, from ServiceRequestSyncWorker.
 */
@Service
@RequiredArgsConstructor
public class ServiceRequestSyncPersistenceService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestEventService eventService;

    /**
     * Atomically records a successful Salesforce sync and updates the request status to SYNCED.
     */
    @Transactional
    public void persistSyncSuccess(ServiceRequest request, String caseId) {
        request.setExternalSyncStatus(ServiceRequestExternalSyncStatus.SYNCED);
        request.setExternalSyncError(null);
        request.setExternalSyncLastAttemptAt(Instant.now());
        eventService.recordSyncSuccess(request.getId(), caseId);
        serviceRequestRepository.save(request);
    }

    /**
     * Atomically records a failed sync attempt and updates the request status to RETRYING or FAILED.
     */
    @Transactional
    public void persistSyncFailure(ServiceRequest request, boolean exhausted, String error) {
        request.setExternalSyncStatus(exhausted
                ? ServiceRequestExternalSyncStatus.FAILED
                : ServiceRequestExternalSyncStatus.RETRYING);
        request.setExternalSyncError(error);
        request.setExternalSyncLastAttemptAt(Instant.now());
        eventService.recordSyncFailure(request.getId(), error);
        serviceRequestRepository.save(request);
    }
}
