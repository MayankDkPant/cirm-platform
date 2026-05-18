package com.cxp.platform.ai.api;

import java.util.UUID;

public record AIComplaintAnalyzeResponse(
        UUID conversationId,
        String suggestedDepartment,
        String suggestedPriority
) {
}
