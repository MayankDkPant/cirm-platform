package com.cirm.platform.complaint.port;

public record CaseUpdateRequest(
       String description,
        String status
) {}
