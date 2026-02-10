package com.cirm.platform.api;

import com.cirm.platform.integration.salesforce.client.SalesforceClient;
import com.cirm.platform.integration.salesforce.dto.CreateCaseSfRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SalesforceTestController {

    private final SalesforceClient salesforceClient;

    @GetMapping("/test/create-case")
    public String createCase() {

        CreateCaseSfRequest request = CreateCaseSfRequest.builder()
                .subject("Water leakage in Sector 12")
                .description("Pipeline broken near park")
                .status("New")
                .origin("Web")
                .build();

        return salesforceClient.createCase(request);
    }
}
