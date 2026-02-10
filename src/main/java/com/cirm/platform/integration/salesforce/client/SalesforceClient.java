package com.cirm.platform.integration.salesforce.client;

import com.cirm.platform.integration.salesforce.dto.CreateCaseSfRequest;
import com.cirm.platform.integration.salesforce.dto.UpdateCaseSfRequest;

public interface SalesforceClient {

    String createCase(CreateCaseSfRequest request);

    void updateCase(String caseId, UpdateCaseSfRequest request);
}
