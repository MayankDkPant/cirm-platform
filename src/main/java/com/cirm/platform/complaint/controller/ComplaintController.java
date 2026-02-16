package com.cirm.platform.complaint.controller;

import com.cirm.platform.complaint.entity.Complaint;
import com.cirm.platform.complaint.service.ComplaintService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;


@Profile("complaint")

@RestController
@RequestMapping("/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    public Complaint createComplaint(@RequestBody Complaint complaint) {
        return complaintService.createComplaint(complaint);
    }

    @GetMapping
    public List<Complaint> getAllComplaints() {
        return complaintService.getAllComplaints();
    }

    @GetMapping("/{id}")
    public Complaint getComplaintById(@PathVariable UUID id) {
    return complaintService.getComplaintById(id);
}

}
