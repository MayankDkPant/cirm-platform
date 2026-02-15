package com.cirm.platform.complaint.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.cirm.platform.common.domain.BaseEntity;
import com.cirm.platform.complaint.entity.enums.ComplaintStatus;

@Entity
@Table(name = "complaint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint extends BaseEntity {


    private String title;
    private String description;
    private String department;
    private String priority;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;

    // Geo
    private Double latitude;
    private Double longitude;

    // External system tracking
    private String externalSystem;         // Case management system 
    private String externalReferenceId;    // Case ID
    private String externalSyncStatus;     // PENDING, SUCCESS, FAILED

    private LocalDateTime resolvedAt;

}
