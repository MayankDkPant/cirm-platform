package com.cirm.platform.complaint.domain;

import com.cirm.platform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Domain entity representing complaint event.
 *
 * Immutable audit trail for complaint lifecycle.
 */
@Entity
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

    @Column(nullable = false)
    private UUID governingBodyId;

    /**
     * User who triggered this event.
     */
    @Column(name = "triggered_by_user_id")
    private UUID createdByUserId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(nullable = false, length = 100)
    private String triggeredBy;
}
