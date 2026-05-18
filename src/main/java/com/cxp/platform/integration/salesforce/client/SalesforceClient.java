package com.cxp.platform.integration.salesforce.client;

import com.cxp.platform.integration.salesforce.dto.UpdateCaseSfRequest;

import java.util.Map;

public interface SalesforceClient {

    String createCase(Map<String, Object> payload);

    void updateCase(String caseId, UpdateCaseSfRequest request);
}
