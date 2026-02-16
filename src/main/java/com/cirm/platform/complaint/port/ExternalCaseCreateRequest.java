package com.cirm.platform.complaint.port;
import org.springframework.context.annotation.Profile;

@Profile("complaint")  

public record ExternalCaseCreateRequest(
        String subject,
        String description,
        String category,
        String priority
       // String citizenId
) {}
