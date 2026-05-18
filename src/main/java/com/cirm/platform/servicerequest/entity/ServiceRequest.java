package com.cirm.platform.servicerequest.entity;

import com.cirm.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "service_request",
        indexes = {
                @Index(name = "idx_service_request_type", columnList = "type"),
                @Index(name = "idx_service_request_status", columnList = "status"),
                @Index(name = "idx_service_request_tenant_created", columnList = "governing_body_id, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceRequestType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 150)
    private String category;

    @Column(length = 50)
    private String priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ServiceRequestStatus status;

    @Column(name = "address_text", columnDefinition = "TEXT")
    private String addressText;

    private Double latitude;
    private Double longitude;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "governing_body_id", nullable = false)
    private UUID governingBodyId;

    @Column(name = "ward_id")
    private UUID wardId;

    @Column(name = "ward_name", length = 255)
    private String wardName;

    @Column(name = "formatted_address", length = 500)
    private String formattedAddress;

    @Column(name = "ai_conversation_id")
    private UUID aiConversationId;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceRequestSource source;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
