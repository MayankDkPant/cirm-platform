package com.cirm.platform.complaint.entity;

import com.cirm.platform.common.domain.BaseEntity;
import com.cirm.platform.complaint.entity.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;

@Entity
@Profile("complaint")   // 👈 load only when complaint module enabled
@Table(name = "complaints",
       indexes = {
           @Index(name = "idx_complaint_status", columnList = "status"),
           @Index(name = "idx_complaint_external_ref", columnList = "externalReferenceId")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

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

    // External system tracking (temporary fields — will move to integration table later)
    @Column(length = 100)
    private String externalSystem;

    @Column(length = 150)
    private String externalReferenceId;

    @Column(length = 50)
    private String externalSyncStatus;

    private LocalDateTime resolvedAt;
}
