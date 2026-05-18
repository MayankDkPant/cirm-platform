package com.cirm.platform.ai.application;

import com.cirm.platform.ai.api.AIComplaintAnalyzeResponse;
import org.springframework.stereotype.Service;

/**
 * Application service orchestrating use cases for the module.
 */
@Service
public class AIConversationService {

    public AIComplaintAnalyzeResponse analyzeComplaintText(String text, Double latitude, Double longitude) {
        throw new UnsupportedOperationException("AI conversation analysis logic is not implemented yet");
    }
}
