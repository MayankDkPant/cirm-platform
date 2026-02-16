package com.cirm.platform.complaint.service;

import com.cirm.platform.ai.client.AiClassificationResponse;
import com.cirm.platform.ai.service.AiClassificationService;
import com.cirm.platform.complaint.entity.Complaint;
import com.cirm.platform.complaint.entity.ComplaintEvent;
import com.cirm.platform.complaint.entity.enums.ComplaintStatus;
import com.cirm.platform.complaint.event.ComplaintCreatedEvent;
import com.cirm.platform.complaint.repository.ComplaintEventRepository;
import com.cirm.platform.complaint.repository.ComplaintRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for complaint lifecycle management.
 *
 * Responsibilities:
 * - Create complaints
 * - Enrich complaints using AI classification
 * - Persist complaint events
 * - Publish domain events for integrations (Salesforce, notifications, etc.)
 *
 * NOTE:
 * This service uses AiClassificationService instead of directly calling AI.
 * This keeps the complaint domain isolated from external integrations.
 */
@Service
@Profile("complaint")
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintEventRepository complaintEventRepository;
    private final AiClassificationService aiClassificationService;
    private final ApplicationEventPublisher eventPublisher;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintEventRepository complaintEventRepository,
            AiClassificationService aiClassificationService,
            ApplicationEventPublisher eventPublisher) {

        this.complaintRepository = complaintRepository;
        this.complaintEventRepository = complaintEventRepository;
        this.aiClassificationService = aiClassificationService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new complaint.
     *
     * Flow:
     * 1) Apply defaults
     * 2) Use AI to auto-classify complaint text
     * 3) Persist complaint
     * 4) Record complaint event
     * 5) Publish domain event for external integrations
     */
    @Transactional
    public Complaint createComplaint(Complaint complaint) {

        // Default status
        if (complaint.getStatus() == null) {
            complaint.setStatus(ComplaintStatus.OPEN);
        }

        // External sync metadata
        complaint.setExternalSystem("SALESFORCE");
        complaint.setExternalSyncStatus("PENDING");

        // AI Classification via AI Service layer
        AiClassificationResponse aiResponse =
                aiClassificationService.classifyComplaint(
                        complaint.getDescription()
                );

        complaint.setDepartment(aiResponse.getDepartment());
        complaint.setPriority(aiResponse.getPriority());

        // Save complaint
        Complaint savedComplaint = complaintRepository.save(complaint);

        // Record event in complaint_event table
        complaintEventRepository.save(
                ComplaintEvent.builder()
                        .complaintId(savedComplaint.getId())
                        .eventType("CREATED")
                        .newValue("OPEN")
                        .triggeredBy("USER")
                        .build()
        );

        // Publish domain event for integrations
        eventPublisher.publishEvent(
                new ComplaintCreatedEvent(savedComplaint.getId())
        );

        return savedComplaint;
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public Complaint getComplaintById(UUID id) {
        return complaintRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Complaint not found with id " + id));
    }

    private void validateStatusTransition(
            ComplaintStatus current,
            ComplaintStatus target) {

        if (current == ComplaintStatus.RESOLVED
                && target != ComplaintStatus.RESOLVED) {

            throw new IllegalStateException(
                    "Cannot change status after resolution");
        }
    }
}
