package com.cxp.platform.integration.salesforce.application;

import com.cxp.platform.integration.salesforce.adapter.SalesforceCaseAdapter;
import com.cxp.platform.servicerequest.entity.ServiceRequest;
import com.cxp.platform.servicerequest.entity.ServiceRequestExternalSyncStatus;
import com.cxp.platform.servicerequest.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Scheduled Salesforce sync worker.
 *
 * CONSISTENCY MODEL: eventual consistency.
 *   service_request rows are persisted locally first (DB-truth), then pushed to
 *   Salesforce asynchronously on a 60-second poll. This intentionally decouples
 *   citizen intake latency from Salesforce availability.
 *
 * ATOMICITY INVARIANT: the audit event (ServiceRequestEvent) and the status update
 *   on ServiceRequest MUST commit together. This is enforced by delegating all
 *   write-back to ServiceRequestSyncPersistenceService, which wraps both writes in
 *   a single @Transactional boundary. Without this, a crash between the two writes
 *   could leave the record in PENDING with a SYNCED event, causing a duplicate push.
 *
 * DISTRIBUTED SCALING GAP: if two instances pick up the same record simultaneously,
 *   both may push to Salesforce. Mitigation (SELECT FOR UPDATE on the poll) is
 *   deferred to the horizontal-scaling phase — acceptable for single-instance now.
 *
 * RETRY SEMANTICS: up to MAX_ATTEMPTS=3. SYNC_FAILED events are appended on each
 *   retry — this is expected audit behavior (each attempt is independently auditable).
 *   After MAX_ATTEMPTS, the record moves to FAILED and exits the poll permanently.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRequestSyncWorker {

    private static final int MAX_ATTEMPTS = 3;

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestPayloadBuilder payloadBuilder;
    private final SalesforceCaseAdapter salesforceCaseAdapter;
    private final ServiceRequestSyncBackoffPolicy backoffPolicy;
    private final ServiceRequestSyncPersistenceService syncPersistence;

    @Scheduled(fixedDelay = 60_000)
    public void syncServiceRequests() {
        List<ServiceRequest> pending = serviceRequestRepository
                .findTop20ByExternalSyncStatusInAndExternalSyncAttemptsLessThan(
                        List.of(
                                ServiceRequestExternalSyncStatus.PENDING,
                                ServiceRequestExternalSyncStatus.RETRYING
                        ),
                        MAX_ATTEMPTS,
                        PageRequest.of(0, 20)
                )
                .getContent();

        if (pending.isEmpty()) {
            return;
        }

        log.info("service_request_sync_started batchSize={}", pending.size());

        for (ServiceRequest request : pending) {
            if (!backoffPolicy.isReadyForRetry(request)) {
                log.info(
                        "service_request_sync_skipped id={} attempts={} lastAttemptAt={}",
                        request.getId(),
                        request.getExternalSyncAttempts(),
                        request.getExternalSyncLastAttemptAt()
                );
                continue;
            }

            int nextAttempt = request.getExternalSyncAttempts() + 1;
            request.setExternalSyncAttempts(nextAttempt);

            try {
                Map<String, Object> payload = payloadBuilder.buildPayload(request);
                // External call outside the transaction boundary — Salesforce failure
                // does not roll back the local status update.
                String caseId = salesforceCaseAdapter.createCase(payload);

                log.info(
                        "service_request_sync_success id={} caseId={} attempts={}",
                        request.getId(), caseId, nextAttempt
                );

                // Atomic: SYNCED status + SYNCED_TO_SALESFORCE event commit together.
                syncPersistence.persistSyncSuccess(request, caseId);

            } catch (Exception ex) {
                boolean exhausted = nextAttempt >= MAX_ATTEMPTS;

                log.error(
                        "service_request_sync_failed id={} attempts={} exhausted={} error={}",
                        request.getId(), nextAttempt, exhausted, ex.getMessage(), ex
                );

                // Atomic: RETRYING/FAILED status + SYNC_FAILED event commit together.
                syncPersistence.persistSyncFailure(request, exhausted, ex.getMessage());
            }
        }
    }
}
