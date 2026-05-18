package com.cxp.platform.ai.api;

import com.cxp.platform.ai.application.AIConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API endpoint for AI-assisted complaint analysis before final submission.
 */
@RestController
@RequestMapping("/api/ai/complaint")
@RequiredArgsConstructor
public class AIComplaintController {

    private final AIConversationService aiConversationService;

    /**
     * Performs AI intent classification and suggestion generation for complaint text.
     * This endpoint analyzes input only and does not create a complaint.
     */
    @PostMapping("/analyze")
    public AIComplaintAnalyzeResponse analyzeComplaint(@Valid @RequestBody AIComplaintAnalyzeRequest request) {
        return aiConversationService.analyzeComplaintText(
                request.text(),
                request.latitude(),
                request.longitude()
        );
    }
}
