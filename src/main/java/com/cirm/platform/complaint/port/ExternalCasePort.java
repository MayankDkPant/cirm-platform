package com.cirm.platform.complaint.port;

public interface ExternalCasePort {

    String createCase(ExternalCaseCreateRequest request);

    void updateCase(String caseId, ExternalCaseUpdateRequest request);
}
