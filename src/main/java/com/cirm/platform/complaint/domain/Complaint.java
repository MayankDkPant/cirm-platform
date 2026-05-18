package com.cirm.platform.complaint.domain;

import com.cirm.platform.common.domain.BaseEntity;
import com.cirm.platform.complaint.domain.enums.ComplaintCreatedVia;
import com.cirm.platform.complaint.domain.enums.ComplaintStatus;
import com.cirm.platform.complaint.domain.enums.ExternalSyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "complaints",
       indexes = {
           @Index(name = "idx_complaint_status", columnList = "status"),
           @Index(name = "idx_complaint_external_ref", columnList = "external_reference")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Domain entity representing complaint.
 *
 * MVP note:
 * Keep this as the single complaint persistence model used by:
 * - complaint application services
 * - Salesforce sync worker
 * - Salesforce payload builder
 */
public class Complaint extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Optional landmark or area description provided by citizen.
     * Used to improve geocoding accuracy and future NLP analysis.
     */
    @Column(columnDefinition = "TEXT")
    private String reportedLocationText;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ComplaintCreatedVia createdVia;

    @Column
    private UUID aiConversationId;

    @Column(length = 150)
    private String department;

    @Column(length = 50)
    private String priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplaintStatus status;

    // Geo location
    private Double latitude;
    private Double longitude;

    /**
     * Routing snapshot derived during complaint creation for audit and analytics.
     */
    @Column(nullable = false)
    private UUID governingBodyId;

    /**
     * Routing snapshot derived during complaint creation for audit and analytics.
     */
    @Column
    private UUID municipalityId;

    @Column(length = 255)
    private String municipalityName;

    @Column
    private UUID wardId;

    @Column(length = 255)
    private String wardName;

    @Column(length = 500)
    private String formattedAddress;

    // External system tracking (temporary fields — will move to integration table later)
    @Column(length = 100)
    private String externalSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_sync_status", nullable = false, length = 30)
    private ExternalSyncStatus externalSyncStatus = ExternalSyncStatus.PENDING;

    @Column(name = "external_sync_attempts", nullable = false)
    private int externalSyncAttempts = 0;

    @Column(name = "external_last_sync_at")
    private Instant externalLastSyncAt;

    @Column(name = "external_reference", length = 50)
    private String externalReference;

    private LocalDateTime resolvedAt;
}
