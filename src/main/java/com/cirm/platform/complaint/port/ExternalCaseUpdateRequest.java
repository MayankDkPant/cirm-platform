package com.cirm.platform.complaint.port;

public record ExternalCaseUpdateRequest(
       String description,
        String status
) {}
