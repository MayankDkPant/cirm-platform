package com.cirm.platform.complaint.api;

import com.cirm.platform.complaint.application.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Temporary controller to handle complaint creation after AI analysis and user confirmation.

/**
 * Handles official complaint submission after user confirmation.
 */
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping("/ai")
    public ComplaintResponse createAIComplaint(@Valid @RequestBody CreateAIComplaintRequest request) {
        return complaintService.createFromAI(request);
    }

    @PostMapping("/manual")
    public ComplaintResponse createManualComplaint(@Valid @RequestBody CreateManualComplaintRequest request) {
        return complaintService.createManual(request);
    }
}
