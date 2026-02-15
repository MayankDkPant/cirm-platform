package com.cirm.platform.complaint.entity;

import com.cirm.platform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "complaint_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintEvent extends BaseEntity {

    @Column(nullable = false)
    private UUID complaintId;

    @Column(nullable = false)
    private String eventType;

    private String oldValue;

    private String newValue;

    @Column(nullable = false)
    private String triggeredBy;
}
