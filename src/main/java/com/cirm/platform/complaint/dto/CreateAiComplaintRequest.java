package com.cirm.platform.complaint.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAiComplaintRequest {

    private String title;
    private String description;
    private Double latitude;
    private Double longitude;
    private UUID aiConversationId;
    private String reportedLocationText;
}
