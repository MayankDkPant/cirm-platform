package com.cirm.platform.complaint.port;

public record CaseRequest(
        String subject,
        String description,
        String category,
        String priority,
        String citizenId
) {}
