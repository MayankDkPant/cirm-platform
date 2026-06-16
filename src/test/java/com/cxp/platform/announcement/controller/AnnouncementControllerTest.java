package com.cxp.platform.announcement.controller;

import com.cxp.platform.announcement.domain.AnnouncementPriority;
import com.cxp.platform.announcement.domain.AnnouncementStatus;
import com.cxp.platform.announcement.domain.AnnouncementTargetScope;
import com.cxp.platform.announcement.dto.AnnouncementResponse;
import com.cxp.platform.announcement.dto.AnnouncementStatusUpdateRequest;
import com.cxp.platform.announcement.dto.AnnouncementUpdateRequest;
import com.cxp.platform.announcement.dto.CitizenAnnouncementDetailResponse;
import com.cxp.platform.announcement.dto.CitizenFeedResponse;
import com.cxp.platform.announcement.exception.AnnouncementNotFoundException;
import com.cxp.platform.announcement.service.AnnouncementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller unit tests for AnnouncementController — full endpoint coverage.
 *
 * Uses standaloneSetup MockMvc — no Spring context, no Flyway, no JPA.
 * Security posture (@PreAuthorize OPERATOR guard) is enforced by Spring Security
 * and is not exercised here; the unit tests verify routing, request mapping, and
 * response serialisation. Security is covered by integration/SecurityConfig tests.
 *
 * Each test group covers:
 *   - Happy path: correct status code, correct response shape
 *   - 404: service throws AnnouncementNotFoundException
 *   - 409: service throws ResponseStatusException(CONFLICT) for invalid transitions
 *   - Field-leak assertions: operator-internal fields must not appear in citizen responses
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AnnouncementService announcementService;

    private static final UUID ID            = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID GOVERNING_ID  = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID CITY_ID       = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID WARD_ID       = UUID.fromString("00000000-0000-0000-0000-000000000030");

    @BeforeEach
    void setUp() {
        AnnouncementController controller = new AnnouncementController(announcementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ── POST /api/v1/announcements ────────────────────────────────────────────

    @Test
    void create_returnsCreatedWithResponse() throws Exception {
        AnnouncementResponse response = makeDraftResponse();
        when(announcementService.create(any())).thenReturn(response);

        String body = """
                {
                  "title": "Water outage",
                  "content": "Water supply disrupted on 22 May.",
                  "targetScope": "CITY",
                  "cityId": "%s"
                }
                """.formatted(CITY_ID);

        mockMvc.perform(post("/api/v1/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    // ── PATCH /api/v1/announcements/{id} ─────────────────────────────────────

    @Test
    void update_draftAnnouncement_returnsOkWithUpdatedResponse() throws Exception {
        AnnouncementResponse updated = makeResponseWith(ID, AnnouncementStatus.DRAFT, "Updated title");
        when(announcementService.update(eq(ID), any(AnnouncementUpdateRequest.class))).thenReturn(updated);

        String body = """
                { "title": "Updated title" }
                """;

        mockMvc.perform(patch("/api/v1/announcements/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void update_announcementNotFound_returns404() throws Exception {
        when(announcementService.update(eq(ID), any(AnnouncementUpdateRequest.class)))
                .thenThrow(new AnnouncementNotFoundException(ID));

        mockMvc.perform(patch("/api/v1/announcements/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_nonDraftAnnouncement_returns409() throws Exception {
        when(announcementService.update(eq(ID), any(AnnouncementUpdateRequest.class)))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Only DRAFT announcements can be updated. Current status: PUBLISHED"));

        mockMvc.perform(patch("/api/v1/announcements/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"new title\" }"))
                .andExpect(status().isConflict());
    }

    @Test
    void update_emptyBody_returnsOk() throws Exception {
        AnnouncementResponse unchanged = makeDraftResponse();
        when(announcementService.update(eq(ID), any(AnnouncementUpdateRequest.class))).thenReturn(unchanged);

        mockMvc.perform(patch("/api/v1/announcements/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_doesNotAcceptTargetingFields() throws Exception {
        // targetScope, wardId etc. are not in AnnouncementUpdateRequest — Jackson ignores unknown fields
        // by default (no @JsonIgnoreProperties(FAIL_ON_UNKNOWN)) so request succeeds;
        // the targeting fields are simply not bound.
        AnnouncementResponse response = makeDraftResponse();
        when(announcementService.update(eq(ID), any(AnnouncementUpdateRequest.class))).thenReturn(response);

        String body = """
                {
                  "title": "New title",
                  "targetScope": "STATE",
                  "stateId": "00000000-0000-0000-0000-0000000000FF"
                }
                """;

        mockMvc.perform(patch("/api/v1/announcements/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    // ── PATCH /api/v1/announcements/{id}/status ───────────────────────────────

    @Test
    void updateStatus_draftToPublished_returnsOkWithPublishedStatus() throws Exception {
        AnnouncementResponse published = makeResponseWith(ID, AnnouncementStatus.PUBLISHED, "Water outage");
        when(announcementService.updateStatus(eq(ID), any(AnnouncementStatusUpdateRequest.class)))
                .thenReturn(published);

        String body = """
                { "status": "PUBLISHED" }
                """;

        mockMvc.perform(patch("/api/v1/announcements/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void updateStatus_publishedToArchived_returnsOkWithArchivedStatus() throws Exception {
        AnnouncementResponse archived = makeResponseWith(ID, AnnouncementStatus.ARCHIVED, "Water outage");
        when(announcementService.updateStatus(eq(ID), any(AnnouncementStatusUpdateRequest.class)))
                .thenReturn(archived);

        String body = """
                { "status": "ARCHIVED" }
                """;

        mockMvc.perform(patch("/api/v1/announcements/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void updateStatus_invalidTransition_returns409() throws Exception {
        when(announcementService.updateStatus(eq(ID), any(AnnouncementStatusUpdateRequest.class)))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Invalid status transition: PUBLISHED → DRAFT"));

        String body = """
                { "status": "DRAFT" }
                """;

        mockMvc.perform(patch("/api/v1/announcements/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatus_alreadySameStatus_returns409() throws Exception {
        when(announcementService.updateStatus(eq(ID), any(AnnouncementStatusUpdateRequest.class)))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Announcement is already PUBLISHED"));

        String body = """
                { "status": "PUBLISHED" }
                """;

        mockMvc.perform(patch("/api/v1/announcements/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatus_announcementNotFound_returns404() throws Exception {
        when(announcementService.updateStatus(eq(ID), any(AnnouncementStatusUpdateRequest.class)))
                .thenThrow(new AnnouncementNotFoundException(ID));

        mockMvc.perform(patch("/api/v1/announcements/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"PUBLISHED\" }"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_missingStatusField_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/announcements/{id}/status", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/v1/announcements ─────────────────────────────────────────────

    @Test
    void list_returnsAllAnnouncementsForTenant() throws Exception {
        when(announcementService.list()).thenReturn(List.of(
                makeDraftResponse(),
                makeResponseWith(UUID.randomUUID(), AnnouncementStatus.PUBLISHED, "Road closure")));

        mockMvc.perform(get("/api/v1/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ── GET /api/v1/announcements/{id} ────────────────────────────────────────

    @Test
    void get_knownId_returnsAnnouncement() throws Exception {
        when(announcementService.get(ID)).thenReturn(makeDraftResponse());

        mockMvc.perform(get("/api/v1/announcements/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void get_unknownId_returns404() throws Exception {
        when(announcementService.get(ID)).thenThrow(new AnnouncementNotFoundException(ID));

        mockMvc.perform(get("/api/v1/announcements/{id}", ID))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/v1/announcements/feed ────────────────────────────────────────

    @Test
    void feed_returnsPublishedListAsCitizenDto() throws Exception {
        CitizenFeedResponse item = makeCitizenResponse(ID, "Water outage");
        when(announcementService.getPublicFeed(null)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/announcements/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ID.toString()))
                .andExpect(jsonPath("$[0].title").value("Water outage"));
    }

    @Test
    void feed_doesNotExposeOperatorFields() throws Exception {
        CitizenFeedResponse item = makeCitizenResponse(ID, "Road closure");
        when(announcementService.getPublicFeed(null)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/announcements/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].governingBodyId").doesNotExist())
                .andExpect(jsonPath("$[0].wardId").doesNotExist())
                .andExpect(jsonPath("$[0].zoneId").doesNotExist())
                .andExpect(jsonPath("$[0].cityId").doesNotExist())
                .andExpect(jsonPath("$[0].districtId").doesNotExist())
                .andExpect(jsonPath("$[0].stateId").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    @Test
    void feed_withWardIdParam_passesThroughToService() throws Exception {
        when(announcementService.getPublicFeed(WARD_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/announcements/feed")
                        .param("wardId", WARD_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── GET /api/v1/announcements/my-feed ─────────────────────────────────────

    @Test
    void myFeed_returnsPersonalisedList() throws Exception {
        CitizenFeedResponse citizenItem = new CitizenFeedResponse(
                ID, "Water outage", "Full body", "Short summary",
                "WATER", AnnouncementPriority.IMPORTANT,
                AnnouncementTargetScope.CITY,
                Instant.now(), null, false, null, Instant.now());

        when(announcementService.getMyFeed()).thenReturn(List.of(citizenItem));

        mockMvc.perform(get("/api/v1/announcements/my-feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ID.toString()));
    }

    @Test
    void myFeed_doesNotExposeOperatorFields() throws Exception {
        CitizenFeedResponse citizenItem = new CitizenFeedResponse(
                ID, "Road closure", "Body", null,
                "ROADS", AnnouncementPriority.NORMAL,
                AnnouncementTargetScope.WARD,
                Instant.now(), null, false, null, Instant.now());

        when(announcementService.getMyFeed()).thenReturn(List.of(citizenItem));

        mockMvc.perform(get("/api/v1/announcements/my-feed"))
                .andExpect(status().isOk())
                // CitizenFeedResponse must not contain these operator-internal fields
                .andExpect(jsonPath("$[0].governingBodyId").doesNotExist())
                .andExpect(jsonPath("$[0].wardId").doesNotExist())
                .andExpect(jsonPath("$[0].zoneId").doesNotExist())
                .andExpect(jsonPath("$[0].cityId").doesNotExist())
                .andExpect(jsonPath("$[0].districtId").doesNotExist())
                .andExpect(jsonPath("$[0].stateId").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    // ── GET /api/v1/announcements/{id}/public ─────────────────────────────────

    @Test
    void getPublic_publishedAnnouncement_returns200WithCitizenFields() throws Exception {
        when(announcementService.getPublicDetail(ID)).thenReturn(makePublicDetailResponse(ID));

        mockMvc.perform(get("/api/v1/announcements/{id}/public", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.title").value("Water outage"))
                .andExpect(jsonPath("$.attachments").isArray());
    }

    @Test
    void getPublic_unknownOrUnpublishedId_returns404() throws Exception {
        when(announcementService.getPublicDetail(ID))
                .thenThrow(new AnnouncementNotFoundException(ID));

        mockMvc.perform(get("/api/v1/announcements/{id}/public", ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPublic_doesNotExposeOperatorFields() throws Exception {
        when(announcementService.getPublicDetail(ID)).thenReturn(makePublicDetailResponse(ID));

        mockMvc.perform(get("/api/v1/announcements/{id}/public", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governingBodyId").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.wardId").doesNotExist())
                .andExpect(jsonPath("$.zoneId").doesNotExist())
                .andExpect(jsonPath("$.cityId").doesNotExist())
                .andExpect(jsonPath("$.districtId").doesNotExist())
                .andExpect(jsonPath("$.stateId").doesNotExist());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CitizenFeedResponse makeCitizenResponse(UUID id, String title) {
        return new CitizenFeedResponse(
                id,
                title,
                "Full announcement body.",
                "Short summary.",
                "WATER",
                AnnouncementPriority.IMPORTANT,
                AnnouncementTargetScope.CITY,
                Instant.now(),
                null,
                false,
                null,
                Instant.now());
    }

    private CitizenAnnouncementDetailResponse makePublicDetailResponse(UUID id) {
        return new CitizenAnnouncementDetailResponse(
                id,
                "Water outage",
                "Short summary.",
                "Full announcement body.",
                "WATER",
                AnnouncementPriority.IMPORTANT,
                AnnouncementTargetScope.CITY,
                Instant.now(),
                null,
                false,
                null,
                Instant.now(),
                List.of()
        );
    }

    private AnnouncementResponse makeDraftResponse() {
        return makeResponseWith(ID, AnnouncementStatus.DRAFT, "Water outage");
    }

    private AnnouncementResponse makeResponseWith(UUID id, AnnouncementStatus status, String title) {
        return new AnnouncementResponse(
                id,
                title,
                "Full announcement body.",
                "Short summary.",
                "WATER",
                AnnouncementPriority.IMPORTANT,
                status,
                GOVERNING_ID,
                AnnouncementTargetScope.CITY,
                null,  // wardId
                null,  // zoneId
                CITY_ID,
                null,  // districtId
                null,  // stateId
                status == AnnouncementStatus.PUBLISHED ? Instant.now() : null,
                null,  // expiresAt
                false,
                null,  // tags
                Instant.now(),
                Instant.now()
        );
    }
}
