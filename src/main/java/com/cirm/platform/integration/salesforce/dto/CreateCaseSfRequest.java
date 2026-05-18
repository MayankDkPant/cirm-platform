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
        String priority,

        @JsonProperty("Municipality_External_Id__c")
        String municipalityExternalId,

        @JsonProperty("Ward_External_Id__c")
        String wardExternalId,

        @JsonProperty("Zone_External_Id__c")
        String zoneExternalId,

        @JsonProperty("Department_External_Id__c")
        String departmentExternalId

        
) {}


