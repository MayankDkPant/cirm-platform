package com.cxp.platform.announcement.controller;

import com.cxp.platform.announcement.dto.AnnouncementCreateRequest;
import com.cxp.platform.announcement.dto.AnnouncementResponse;
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
            description = "Returns published, active, non-expired announcements. No authentication required. " +
                          "Optionally filter by wardId to narrow to a specific ward's feed. " +
                          "Authenticated citizens with a stored profile should use /my-feed for personalised results."
    )
    @ApiResponse(responseCode = "200", description = "Public announcement feed")
    // Public — no Bearer JWT required. Overrides the global bearerAuth security requirement.
    @SecurityRequirements({})
    @GetMapping("/feed")
    public List<AnnouncementResponse> feed(@RequestParam(required = false) UUID wardId) {
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

    @Operation(summary = "Retrieve an announcement")
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Announcement returned")
    @ApiResponse(responseCode = "404",
            description = "Announcement not found for the given ID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    public AnnouncementResponse get(@PathVariable UUID id) {
        return announcementService.get(id);
    }
}
