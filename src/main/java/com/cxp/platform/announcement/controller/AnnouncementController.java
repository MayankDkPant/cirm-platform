package com.cxp.platform.announcement.controller;

import com.cxp.platform.announcement.dto.AnnouncementCreateRequest;
import com.cxp.platform.announcement.dto.AnnouncementResponse;
import com.cxp.platform.announcement.dto.AnnouncementStatusUpdateRequest;
import com.cxp.platform.announcement.dto.AnnouncementUpdateRequest;
import com.cxp.platform.announcement.dto.CitizenAnnouncementDetailResponse;
import com.cxp.platform.announcement.dto.CitizenFeedResponse;
import com.cxp.platform.announcement.service.AnnouncementService;
import com.cxp.platform.api.error.ApiError;
import com.cxp.platform.common.openapi.StandardApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Announcements")
@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(
            summary     = "Create an announcement",
            description = "Publishes a civic announcement scoped to the operator's tenant. " +
                          "Targeting (ward, city, district, or state) is set in the request body. " +
                          "Operator-only — requires the OPERATOR role on the platform JWT."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "201", description = "Announcement created")
    @ApiResponse(responseCode = "400",
            description = "Validation failure — required fields missing, invalid targeting, or targeting conflicts.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403",
            description = "Forbidden — the caller does not have the OPERATOR role.",
            content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPERATOR')")
    public AnnouncementResponse create(@Valid @RequestBody AnnouncementCreateRequest request) {
        return announcementService.create(request);
    }

    @Operation(
            summary     = "Update a draft announcement",
            description = "Updates the mutable content fields of a DRAFT announcement. " +
                          "Null fields are ignored — only supplied fields are overwritten. " +
                          "Targeting fields (targetScope, wardId, zoneId, cityId, districtId, stateId) " +
                          "are immutable after creation and are not accepted by this endpoint. " +
                          "Returns HTTP 409 if the announcement is not in DRAFT status. " +
                          "Operator-only — requires the OPERATOR role on the platform JWT."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Announcement updated")
    @ApiResponse(responseCode = "400",
            description = "Validation failure — field value violates a constraint (e.g. expires_at in the past).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403",
            description = "Forbidden — the caller does not have the OPERATOR role.",
            content = @Content)
    @ApiResponse(responseCode = "404",
            description = "Announcement not found for the given ID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409",
            description = "Conflict — announcement is not in DRAFT status. Use PATCH /status to transition the lifecycle.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public AnnouncementResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody AnnouncementUpdateRequest request) {
        return announcementService.update(id, request);
    }

    @Operation(
            summary     = "Transition announcement lifecycle status",
            description = "Advances the announcement's status along the operator lifecycle. " +
                          "Valid transitions: DRAFT → PUBLISHED, PUBLISHED → ARCHIVED. " +
                          "Publishing sets publishedAt to the current server timestamp and makes the " +
                          "announcement visible in citizen feeds immediately. " +
                          "Archiving removes the announcement from all citizen feeds and retains it for audit. " +
                          "EXPIRED is system-managed and cannot be set via this endpoint. " +
                          "Operator-only — requires the OPERATOR role on the platform JWT."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Status updated; updated announcement returned")
    @ApiResponse(responseCode = "400",
            description = "Validation failure — status field missing or EXPIRED supplied.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403",
            description = "Forbidden — the caller does not have the OPERATOR role.",
            content = @Content)
    @ApiResponse(responseCode = "404",
            description = "Announcement not found for the given ID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409",
            description = "Conflict — the requested transition is not valid from the current status. " +
                          "Response body includes current status and allowed next states.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OPERATOR')")
    public AnnouncementResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AnnouncementStatusUpdateRequest request) {
        return announcementService.updateStatus(id, request);
    }

    @Operation(
            summary     = "List all announcements",
            description = "Returns all announcements for the operator's tenant, including drafts and expired entries. " +
                          "Operator-only — citizens use /feed or /my-feed instead."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "All announcements for the operator's tenant")
    @ApiResponse(responseCode = "403",
            description = "Forbidden — the caller does not have the OPERATOR role.",
            content = @Content)
    @GetMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public List<AnnouncementResponse> list() {
        return announcementService.list();
    }

    @Operation(
            summary     = "Browse the public announcement feed",
            description = "Returns published, active, non-expired announcements in a citizen-safe shape. " +
                          "Internal governance fields (governingBodyId, wardId, zoneId, cityId, districtId, " +
                          "stateId, status) are not included in the response. " +
                          "No authentication required. " +
                          "Optionally filter by wardId to narrow to a specific ward's feed. " +
                          "Authenticated citizens with a stored profile should use /my-feed for personalised results."
    )
    @ApiResponse(responseCode = "200", description = "Public announcement feed")
    // Public — no Bearer JWT required. Overrides the global bearerAuth security requirement.
    @SecurityRequirements({})
    @GetMapping("/feed")
    public List<CitizenFeedResponse> feed(@RequestParam(required = false) UUID wardId) {
        return announcementService.getPublicFeed(wardId);
    }

    @Operation(
            summary     = "Retrieve the personalised citizen announcement feed",
            description = "Returns published, active, non-expired announcements relevant to the authenticated citizen. " +
                          "Geography (ward, city, district, state) is resolved from the caller's stored profile — " +
                          "no geography parameters are accepted. Requires: authenticated citizen session."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Personalised announcement feed")
    @GetMapping("/my-feed")
    public List<CitizenFeedResponse> myFeed() {
        return announcementService.getMyFeed();
    }

    @Operation(
            summary     = "Retrieve an announcement",
            description = "Returns the full operator-facing representation of a single announcement, " +
                          "including internal routing fields (governingBodyId, geographic FKs, status). " +
                          "Operator-only — requires the OPERATOR role on the platform JWT. " +
                          "Citizens seeking announcement detail should use GET /feed or GET /my-feed."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Announcement returned")
    @ApiResponse(responseCode = "403",
            description = "Forbidden — the caller does not have the OPERATOR role.",
            content = @Content)
    @ApiResponse(responseCode = "404",
            description = "Announcement not found for the given ID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public AnnouncementResponse get(@PathVariable UUID id) {
        return announcementService.get(id);
    }

    @Operation(
            summary     = "Retrieve a published announcement (citizen view)",
            description = "Returns citizen-safe detail for a single published, active, non-expired announcement. " +
                          "Authentication required — both Supabase citizen JWTs and platform operator JWTs are accepted. " +
                          "DRAFT, archived, expired, and unknown IDs all return 404; the caller cannot determine " +
                          "whether an ID exists but is in a non-public state. " +
                          "Internal governance and routing fields (governingBodyId, wardId, zoneId, cityId, " +
                          "districtId, stateId, status) are never included in the response. " +
                          "Citizens use this to retrieve an announcement they found via /feed or /my-feed."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Announcement detail returned")
    @ApiResponse(responseCode = "401",
            description = "Unauthorized — no valid Bearer JWT supplied.",
            content = @Content)
    @ApiResponse(responseCode = "404",
            description = "Announcement not found, not yet published, expired, or archived.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}/public")
    public CitizenAnnouncementDetailResponse getPublic(@PathVariable UUID id) {
        return announcementService.getPublicDetail(id);
    }
}
