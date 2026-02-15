package com.cirm.platform.complaint.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.cirm.platform.complaint.repository.*;
import com.cirm.platform.complaint.port.*;
import com.cirm.platform.complaint.entity.Complaint;
import com.cirm.platform.complaint.event.ComplaintCreatedEvent;



@Component
@RequiredArgsConstructor
public class ExternalCaseSyncListener {

    private final ComplaintRepository complaintRepository;
    private final ExternalCasePort externalCasePort;

    @EventListener
    public void handleComplaintCreated(ComplaintCreatedEvent event) {

        Complaint complaint = complaintRepository
                .findById(event.complaintId())
                .orElseThrow();

        try {

            ExternalCaseCreateRequest request =
                    new ExternalCaseCreateRequest(
                            complaint.getTitle(),
                            complaint.getDescription(),
                            complaint.getDepartment(),
                            complaint.getPriority()
                    );

            String externalId = 
                externalCasePort.createCase(request);

            complaint.setExternalReferenceId(externalId);
            complaint.setExternalSyncStatus("SUCCESS");

        } catch (Exception ex) {
            complaint.setExternalSyncStatus("FAILED");
        }

        complaintRepository.save(complaint);
    }
}
