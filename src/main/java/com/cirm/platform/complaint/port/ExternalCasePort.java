package com.cirm.platform.complaint.port;
import org.springframework.context.annotation.Profile;

@Profile("complaint")  

public interface ExternalCasePort {

    String createCase(ExternalCaseCreateRequest request);

    void updateCase(String caseId, ExternalCaseUpdateRequest request);
}
