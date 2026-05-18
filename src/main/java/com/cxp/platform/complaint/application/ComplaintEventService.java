package com.cxp.platform.complaint.application;

import com.cxp.platform.auth.application.CurrentUserProvider;
import com.cxp.platform.complaint.domain.Complaint;
import com.cxp.platform.complaint.domain.ComplaintEvent;
import com.cxp.platform.complaint.repository.ComplaintEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Application service orchestrating use cases for the module.
 *
 * Complaint events are immutable audit records.
 * Events can only be created, never updated or deleted.
 */
@Service
@RequiredArgsConstructor
public class ComplaintEventService {

    private final ComplaintEventRepository complaintEventRepository;
    private final CurrentUserProvider currentUserProvider;

    public ComplaintEvent createEvent(ComplaintEvent event) {
        return complaintEventRepository.save(event);
    }

    /**
     * Creates immutable CREATED event with actor identity.
     */
    public ComplaintEvent createCreatedEvent(Complaint complaint) {
        java.util.UUID userId = currentUserProvider.getCurrentUserId();

        return complaintEventRepository.save(
                ComplaintEvent.builder()
                        .complaintId(complaint.getId())
                        .governingBodyId(complaint.getGoverningBodyId())
                        .createdByUserId(userId)
                        .eventType("CREATED")
                        .newValue("OPEN")
                        .triggeredBy("USER")
                        .build()
        );
    }
}
