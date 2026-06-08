package com.cxp.platform.announcement.dto;

import com.cxp.platform.announcement.domain.AnnouncementPriority;
import com.cxp.platform.announcement.domain.AnnouncementTargetScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Citizen-safe detail view for a single published announcement.
 *
 * Deliberately omits all operator and governance fields:
 *   - governingBodyId, wardId, zoneId, cityId, districtId, stateId — internal routing FKs
 *   - status — always PUBLISHED if this DTO is returned; no value in exposing it
 *   - updatedAt — operator audit field
 *   - createdByUserId, updatedByUserId — actor audit fields
 *
 * targetScope is the geographic audience label (e.g. CITY, WARD) — safe to expose because
 * it describes audience reach, not internal routing infrastructure.
 */
@Schema(description = "Citizen-safe announcement detail. Never contains operator or governance fields.")
public record CitizenAnnouncementDetailResponse(
        UUID id,
        String title,
        String summary,
        String content,
        String category,
        AnnouncementPriority priority,
        AnnouncementTargetScope targetScope,
        Instant publishedAt,
        Instant expiresAt,
        boolean pinned,
        String tags,
        Instant createdAt,

        @Schema(description = "Media attachments. Reserved for future Cloudflare media integration — always empty in this release.")
        List<AttachmentRef> attachments
) {}
