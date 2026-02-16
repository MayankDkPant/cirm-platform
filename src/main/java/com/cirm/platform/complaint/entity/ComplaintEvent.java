package com.cirm.platform.complaint.entity;

import com.cirm.platform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@Entity
@Profile("complaint")   // 👈 load only when complaint module enabled
@Table(name = "complaint_event",
       indexes = {
           @Index(name = "idx_event_complaint", columnList = "complaintId"),
           @Index(name = "idx_event_type", columnList = "eventType")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintEvent extends BaseEntity {

    @Column(nullable = false)
    private UUID complaintId;   // FK will be added via Flyway later

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(nullable = false, length = 100)
    private String triggeredBy;
}
