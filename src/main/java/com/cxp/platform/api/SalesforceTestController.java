package com.cxp.platform.api;

import com.cxp.platform.integration.salesforce.client.SalesforceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SalesforceTestController {

    private final SalesforceClient salesforceClient;

    @GetMapping("/test/create-case")
    public String createCase() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("Subject", "Water leakage in Sector 12");
        payload.put("Description", "Pipeline broken near park");
        payload.put("Status", "New");
        payload.put("Origin", "Web");
        return salesforceClient.createCase(payload);
    }
}
