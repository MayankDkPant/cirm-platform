package com.cxp.platform.integration.salesforce.adapter;

import com.cxp.platform.integration.salesforce.client.SalesforceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SalesforceCaseAdapter {

    private final SalesforceClient salesforceClient;

    public String createCase(Map<String, Object> payload) {
        return salesforceClient.createCase(payload);
    }
}
