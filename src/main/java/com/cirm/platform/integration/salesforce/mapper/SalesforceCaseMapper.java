package com.cirm.platform.integration.salesforce.mapper;

import com.cirm.platform.complaint.port.ExternalCaseCreateRequest;
import com.cirm.platform.complaint.port.ExternalCaseUpdateRequest;
import com.cirm.platform.integration.salesforce.dto.CreateCaseSfRequest;
import com.cirm.platform.integration.salesforce.dto.UpdateCaseSfRequest;
import org.springframework.stereotype.Component;

@Component
public class SalesforceCaseMapper {

    public CreateCaseSfRequest toSalesforceRequest(ExternalCaseCreateRequest request) {

        return new CreateCaseSfRequest(
                request.subject(),
                request.description(),
                "New",
                "Web",
                request.category(),
                request.priority()
        );
    }

    public UpdateCaseSfRequest toSalesforceUpdate(ExternalCaseUpdateRequest request) {
        return new UpdateCaseSfRequest(
                request.description(),
                request.status()
        );
    }
}
