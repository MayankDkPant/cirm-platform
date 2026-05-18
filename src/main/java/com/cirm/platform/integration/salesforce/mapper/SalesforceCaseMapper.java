package com.cirm.platform.integration.salesforce.mapper;

import com.cirm.platform.complaint.port.ExternalCaseCreateRequest;
import com.cirm.platform.complaint.port.ExternalCaseUpdateRequest;
import com.cirm.platform.integration.salesforce.dto.UpdateCaseSfRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SalesforceCaseMapper {

    public Map<String, Object> toSalesforceRequest(ExternalCaseCreateRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("Subject", request.subject());
        payload.put("Description", request.description());
        payload.put("Status", "New");
        payload.put("Origin", "Web");
        payload.put("Department__c", request.category());
        payload.put("Priority", request.priority());
        return payload;
    }

    public UpdateCaseSfRequest toSalesforceUpdate(ExternalCaseUpdateRequest request) {
        return new UpdateCaseSfRequest(
                request.description(),
                request.status()
        );
    }
}
