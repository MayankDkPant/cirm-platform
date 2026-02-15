package com.cirm.platform.complaint.port;

public record ExternalCaseCreateRequest(
        String subject,
        String description,
        String category,
        String priority
       // String citizenId
) {}
