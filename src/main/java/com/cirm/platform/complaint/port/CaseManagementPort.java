package com.cirm.platform.complaint.port;

public interface CaseManagementPort {

    String createCase(CaseRequest request);

    void updateCase(String caseId, CaseUpdateRequest request);
}
