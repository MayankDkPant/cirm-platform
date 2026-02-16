package com.cirm.platform.complaint.port;
import org.springframework.context.annotation.Profile;

@Profile("complaint")  
public record ExternalCaseUpdateRequest(
       String description,
        String status
) {}
