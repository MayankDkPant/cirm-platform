package com.cxp.platform.complaint.port;

public record ExternalCaseUpdateRequest(
       String description,
        String status
) {}
