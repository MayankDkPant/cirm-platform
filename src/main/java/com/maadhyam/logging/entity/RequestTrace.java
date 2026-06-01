package com.maadhyam.logging.entity;

import com.maadhyam.logging.enums.RequestState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "request_traces",
        indexes = {
                @Index(name = "idx_request_traces_correlation_id", columnList = "correlation_id"),
                @Index(name = "idx_request_traces_timestamp", columnList = "request_timestamp"),
                @Index(name = "idx_request_traces_state_timestamp", columnList = "request_state, request_timestamp"),
                @Index(name = "idx_request_traces_user_timestamp", columnList = "user_id, request_timestamp")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_state", nullable = false, length = 30)
    private RequestState requestState;

    @Column(name = "request_timestamp", nullable = false)
    private Instant requestTimestamp;

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Column(name = "http_method", length = 20)
    private String httpMethod;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "ward_id")
    private UUID wardId;

    @Column(name = "governing_body_id")
    private UUID governingBodyId;

    @Column(name = "jwt_subject", length = 255)
    private String jwtSubject;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "source_platform", length = 50)
    private String sourcePlatform;

    @Column(name = "app_version", length = 50)
    private String appVersion;
}
