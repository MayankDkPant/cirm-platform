package com.cirm.platform.complaint.service;

import com.cirm.platform.complaint.entity.Complaint;
import com.cirm.platform.complaint.entity.enums.ComplaintStatus;
import com.cirm.platform.complaint.repository.ComplaintRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.cirm.platform.ai.AiClient;
import com.cirm.platform.ai.AiClassificationResponse;

import  com.cirm.platform.complaint.port.*;

import java.time.LocalDateTime;
import java.util.List;



@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final AiClient aiClient;
    private final CaseManagementPort caseManagementPort;


    public ComplaintService(ComplaintRepository complaintRepository,
                            AiClient aiClient,
                            CaseManagementPort caseManagementPort) {
        this.complaintRepository = complaintRepository;
        this.aiClient  = aiClient;
        this.caseManagementPort = caseManagementPort;
    }

    public Complaint createComplaint(Complaint complaint) {
        System.out.println("SERVICE METHOD EXECUTED");
        System.out.println("complaint.getStatus() = " + complaint.getStatus());

        // Default status if not provided
        if (complaint.getStatus() == null) {
            complaint.setStatus(ComplaintStatus.OPEN);
        }

        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());

        // 1 AI Classification
        AiClassificationResponse aiResponse =
                aiClient.classify(complaint.getDescription());

        // Enrich complaint
        complaint.setDepartment(aiResponse.getDepartment());
        complaint.setPriority(aiResponse.getPriority());

        System.out.println("AI Department: " + aiResponse.getDepartment());
        System.out.println("AI Priority: " + aiResponse.getPriority());

        // 2 Save locally
        Complaint savedComplaint = complaintRepository.save(complaint);

        // 3 Call port (NOT Salesforce directly)
        CaseRequest caseRequest = new CaseRequest(
                "Complaint - " + savedComplaint.getDepartment(),
                savedComplaint.getDescription(),
                savedComplaint.getDepartment(),
                savedComplaint.getPriority(),
                null    
        );
        caseManagementPort.createCase(caseRequest);

        return savedComplaint;
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }
    
public Complaint getComplaintById(Long id) {
    return complaintRepository.findById(id)
            .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Complaint not found with id " + id));
}
}
