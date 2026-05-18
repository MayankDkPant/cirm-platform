package com.cirm.platform.integration.salesforce.adapter;

import com.cirm.platform.complaint.port.ExternalCasePort;
import com.cirm.platform.complaint.port.ExternalCaseCreateRequest;
import com.cirm.platform.complaint.port.ExternalCaseUpdateRequest;
import com.cirm.platform.integration.salesforce.client.SalesforceClient;
import com.cirm.platform.integration.salesforce.dto.UpdateCaseSfRequest;
import com.cirm.platform.integration.salesforce.mapper.SalesforceCaseMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesforceCaseAdapter implements ExternalCasePort {

    private final SalesforceClient salesforceClient;
    private final SalesforceCaseMapper mapper;

    public SalesforceCaseAdapter(SalesforceClient salesforceClient,
                                 SalesforceCaseMapper mapper) {
        this.salesforceClient = salesforceClient;
        this.mapper = mapper;
    }

    @Override
    public String createCase(ExternalCaseCreateRequest request) {
        return salesforceClient.createCase(mapper.toSalesforceRequest(request));
    }

    public String createCase(Map<String, Object> payload) {
        return salesforceClient.createCase(payload);
    }
    @Override
    public void updateCase(String caseId, ExternalCaseUpdateRequest request) {
        UpdateCaseSfRequest sfRequest = mapper.toSalesforceUpdate(request);

    salesforceClient.updateCase(caseId, sfRequest);
    }

}

