package com.cirm.platform.integration.salesforce.mapper;

import com.cirm.platform.complaint.port.CaseRequest;
import com.cirm.platform.complaint.port.CaseUpdateRequest;
import com.cirm.platform.integration.salesforce.dto.CreateCaseSfRequest;
import com.cirm.platform.integration.salesforce.dto.UpdateCaseSfRequest;
import org.springframework.stereotype.Component;

@Component
public class SalesforceCaseMapper {

    public CreateCaseSfRequest toSalesforceRequest(CaseRequest request) {

        return new CreateCaseSfRequest(
                request.subject(),
                request.description(),
                "New",
                "Web",
                request.category(),
                request.priority()
        );
    }

    public UpdateCaseSfRequest toSalesforceUpdate(CaseUpdateRequest request) {
        return new UpdateCaseSfRequest(
                request.description(),
                request.status()
        );
    }
}
