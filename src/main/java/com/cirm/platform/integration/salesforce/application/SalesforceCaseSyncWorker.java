package com.cirm.platform.integration.salesforce.application;

import com.cirm.platform.complaint.application.ExternalSyncBackoffPolicy;
import com.cirm.platform.complaint.domain.Complaint;
import com.cirm.platform.complaint.domain.enums.ExternalSyncStatus;
import com.cirm.platform.complaint.repository.ComplaintRepository;
import com.cirm.platform.integration.salesforce.adapter.SalesforceCaseAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@EnableAsync
@RequiredArgsConstructor
public class SalesforceCaseSyncWorker {

    private final ComplaintRepository complaintRepository;
    private final SalesforceCasePayloadBuilder payloadBuilder;
    private final SalesforceCaseAdapter salesforceCaseAdapter;
    private final ExternalSyncBackoffPolicy backoffPolicy;

    @Scheduled(fixedDelay = 60_000)
    public void syncComplaints() {
        List<Complaint> complaints = complaintRepository
                .findTop20ByExternalSyncStatusInAndExternalSyncAttemptsLessThan(
                        List.of(ExternalSyncStatus.PENDING, ExternalSyncStatus.FAILED),
                        3,
                        PageRequest.of(0, 20)
                )
                .getContent();

        if (complaints.isEmpty()) {
            return;
        }

        log.info("salesforce_sync_started batchSize={}", complaints.size());

        for (Complaint complaint : complaints) {
            if (!backoffPolicy.isReadyForRetry(complaint)) {
                log.info(
                        "salesforce_retry_scheduled complaintId={} attempts={} lastSyncAt={}",
                        complaint.getId(),
                        complaint.getExternalSyncAttempts(),
                        complaint.getExternalLastSyncAt()
                );
                continue;
            }

            int nextAttempt = complaint.getExternalSyncAttempts() + 1;
            complaint.setExternalSyncAttempts(nextAttempt);

            try {
                Map<String, Object> payload = payloadBuilder.buildPayload(complaint);
                String caseId = salesforceCaseAdapter.createCase(payload);

                complaint.setExternalSyncStatus(ExternalSyncStatus.SUCCESS);
                complaint.setExternalReference(caseId);

                log.info(
                        "salesforce_sync_success complaintId={} caseId={} attempts={}",
                        complaint.getId(),
                        caseId,
                        nextAttempt
                );
            } catch (Exception ex) {
                complaint.setExternalSyncStatus(ExternalSyncStatus.FAILED);

                log.error(
                        "salesforce_sync_failed complaintId={} attempts={} error={}",
                        complaint.getId(),
                        nextAttempt,
                        ex.getMessage(),
                        ex
                );

                if (nextAttempt < 3) {
                    log.info(
                            "salesforce_retry_scheduled complaintId={} nextAttempt={}",
                            complaint.getId(),
                            nextAttempt + 1
                    );
                }
            }

            complaint.setExternalLastSyncAt(Instant.now());
            complaintRepository.save(complaint);
        }
    }
}
