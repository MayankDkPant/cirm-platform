package com.cirm.platform.complaint.service;

import com.cirm.platform.complaint.entity.Complaint;
import com.cirm.platform.complaint.entity.ComplaintEvent;
import com.cirm.platform.complaint.entity.enums.ComplaintStatus;
import com.cirm.platform.complaint.repository.ComplaintRepository;
import com.cirm.platform.complaint.repository.ComplaintEventRepository;
import com.cirm.platform.complaint.event.ComplaintCreatedEvent;
import com.cirm.platform.ai.AiClient;
import com.cirm.platform.ai.AiClassificationResponse;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintEventRepository complaintEventRepository;
    private final AiClient aiClient;
    private final ApplicationEventPublisher eventPublisher;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintEventRepository complaintEventRepository,
            AiClient aiClient,
            ApplicationEventPublisher eventPublisher) {

        this.complaintRepository = complaintRepository;
        this.complaintEventRepository = complaintEventRepository;
        this.aiClient = aiClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Complaint createComplaint(Complaint complaint) {

        // 1️⃣ Default status
        if (complaint.getStatus() == null) {
            complaint.setStatus(ComplaintStatus.OPEN);
        }

        // 2️⃣ External sync metadata
        complaint.setExternalSystem("SALESFORCE");
        complaint.setExternalSyncStatus("PENDING");

        // 3️⃣ AI Classification
        AiClassificationResponse aiResponse =
                aiClient.classify(complaint.getDescription());

        complaint.setDepartment(aiResponse.getDepartment());
        complaint.setPriority(aiResponse.getPriority());

        // 4️⃣ Save complaint
        Complaint savedComplaint = complaintRepository.save(complaint);

        // 5️⃣ Record event
        complaintEventRepository.save(
                ComplaintEvent.builder()
                        .complaintId(savedComplaint.getId())
                        .eventType("CREATED")
                        .newValue("OPEN")
                        .triggeredBy("USER")
                        .build()
        );

        // 6️⃣ Publish domain event
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
