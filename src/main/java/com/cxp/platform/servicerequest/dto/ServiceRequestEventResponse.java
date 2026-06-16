package com.cxp.platform.servicerequest.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Single entry in a service request's immutable audit timeline.
 *
 * Canonical field names (Sprint 2 / PR2):
 *   oldStatus      — status before the transition (was: oldValue)
 *   newStatus      — status after the transition (was: newValue)
 *   actorType      — who performed the action: USER, OPERATOR, SYSTEM (was: changedByRole)
 *   actorUserId    — platform userId of the actor; null for SYSTEM events (was: silently dropped)
 *   notes          — human-readable context: sync reference ID, error message, etc. (was: silently dropped)
 *   metadataJson   — structured enrichment snapshot; populated on CREATED events with AI
 *                    classification metadata (category, priority, confidence, routing) (was: silently dropped)
 *
 * Transitional compatibility aliases (deprecated — remove after client migration):
 *   oldValue, newValue, changedByRole — kept for one release cycle for existing clients.
 */
@Schema(description = "Single immutable entry in a service request's audit timeline. " +
                      "Events are append-only and represent every state transition and sync outcome. " +
                      "The deprecated aliases (oldValue, newValue, changedByRole) are emitted alongside " +
                      "the canonical fields for backward compatibility and will be removed once clients migrate.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestEventResponse {

    @Schema(description = "Stable identifier for this audit event.")
    private UUID id;

    @Schema(description = "Event classification: CREATED, STATUS_UPDATED, SYNCED_TO_SALESFORCE, SYNC_FAILED.")
    private String eventType;

    @Schema(description = "Lifecycle status before this transition. Null for CREATED events.", nullable = true)
    private String oldStatus;

    @Schema(description = "Lifecycle status after this transition. Always populated.")
    private String newStatus;

    @Schema(description = "Actor category responsible for the event: USER (citizen submit), " +
                          "OPERATOR (status change), SYSTEM (sync worker). Always populated.")
    private String actorType;

    @Schema(description = "Platform userId of the actor. Null for SYSTEM-initiated events (sync worker).",
            nullable = true)
    private UUID actorUserId;

    @Schema(description = "Human-readable event context. " +
                          "SYNCED_TO_SALESFORCE: external case reference (e.g. '00001234'). " +
                          "SYNC_FAILED: error description. Null for CREATED and STATUS_UPDATED events.",
            nullable = true)
    private String notes;

    @Schema(description = "Structured AI enrichment snapshot (JSON string). " +
                          "Populated only on CREATED events — records category, priority, confidence, and ward routing " +
                          "at submission time. This is the sole durable audit record of how the request was classified. " +
                          "Advisory/non-authoritative. Null for all other event types.",
            nullable = true)
    private String metadataJson;

    @Schema(description = "UTC timestamp when this event was recorded. Always populated.")
    private Instant createdAt;

    // ── Transitional backward-compat aliases ─────────────────────────────────
    // These duplicate canonical fields above. Both are emitted simultaneously.
    // Remove after all clients migrate to the canonical field names.

    /** @deprecated use {@code oldStatus} — will be removed in a future release */
    @Deprecated
    @Schema(description = "DEPRECATED — use oldStatus. " +
                          "Backward-compatibility alias retained for one release cycle. " +
                          "Will be removed in a future release.",
            deprecated = true, nullable = true)
    @JsonGetter("oldValue")
    public String getOldValue() { return oldStatus; }

    /** @deprecated use {@code newStatus} — will be removed in a future release */
    @Deprecated
    @Schema(description = "DEPRECATED — use newStatus. " +
                          "Backward-compatibility alias retained for one release cycle. " +
                          "Will be removed in a future release.",
            deprecated = true)
    @JsonGetter("newValue")
    public String getNewValue() { return newStatus; }

    /** @deprecated use {@code actorType} — will be removed in a future release */
    @Deprecated
    @Schema(description = "DEPRECATED — use actorType. " +
                          "Backward-compatibility alias retained for one release cycle. " +
                          "Will be removed in a future release.",
            deprecated = true)
    @JsonGetter("changedByRole")
    public String getChangedByRole() { return actorType; }
}
