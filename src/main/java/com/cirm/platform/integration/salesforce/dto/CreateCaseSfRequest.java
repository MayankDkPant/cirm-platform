package com.cirm.platform.integration.salesforce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record CreateCaseSfRequest(

        @JsonProperty("Subject")
        String subject,

        @JsonProperty("Description")
        String description,

        @JsonProperty("Status")
        String status,

        @JsonProperty("Origin")
        String origin,

        @JsonProperty("Department__c")
        String department,

        @JsonProperty("Priority")
        String priority

        
) {}



