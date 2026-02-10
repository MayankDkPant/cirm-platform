package com.cirm.platform.integration.salesforce.adapter;

import com.cirm.platform.complaint.port.CaseManagementPort;
import com.cirm.platform.complaint.port.CaseRequest;
import com.cirm.platform.complaint.port.CaseUpdateRequest;
import com.cirm.platform.integration.salesforce.client.SalesforceClient;
import com.cirm.platform.integration.salesforce.dto.CreateCaseSfRequest;
import com.cirm.platform.integration.salesforce.dto.UpdateCaseSfRequest;
import com.cirm.platform.integration.salesforce.mapper.SalesforceCaseMapper;
import org.springframework.stereotype.Component;

@Component
public class SalesforceCaseAdapter implements CaseManagementPort {

    private final SalesforceClient salesforceClient;
    private final SalesforceCaseMapper mapper;

    public SalesforceCaseAdapter(SalesforceClient salesforceClient,
                                 SalesforceCaseMapper mapper) {
        this.salesforceClient = salesforceClient;
        this.mapper = mapper;
    }

    @Override
    public String createCase(CaseRequest request) {

        CreateCaseSfRequest sfRequest = mapper.toSalesforceRequest(request);

        return salesforceClient.createCase(sfRequest);
    }
    @Override
    public void updateCase(String caseId, CaseUpdateRequest request) {
        UpdateCaseSfRequest sfRequest = mapper.toSalesforceUpdate(request);

    salesforceClient.updateCase(caseId, sfRequest);
    }

}


