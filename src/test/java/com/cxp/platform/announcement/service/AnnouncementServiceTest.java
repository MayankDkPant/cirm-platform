package com.cxp.platform.announcement.service;

import com.cxp.platform.announcement.domain.Announcement;
import com.cxp.platform.announcement.domain.AnnouncementPriority;
import com.cxp.platform.announcement.domain.AnnouncementStatus;
import com.cxp.platform.announcement.domain.AnnouncementTargetScope;
import com.cxp.platform.announcement.dto.AnnouncementStatusUpdateRequest;
import com.cxp.platform.announcement.dto.AnnouncementUpdateRequest;
import com.cxp.platform.announcement.dto.CitizenFeedResponse;
import com.cxp.platform.announcement.exception.AnnouncementNotFoundException;
import com.cxp.platform.announcement.repository.AnnouncementRepository;
import com.cxp.platform.auth.application.CurrentUserProvider;
import com.cxp.platform.auth.infrastructure.persistence.UserProfileRepository;
import com.cxp.platform.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnnouncementService — focuses on the lifecycle business rules:
 *
 *   update()       — mutable field patching, DRAFT-only guard, immutable-field exclusion
 *   updateStatus() — valid transitions, invalid transitions, publishedAt assignment
 *
 * TenantContext is seeded directly (ThreadLocal) and cleared after each test.
 * No Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private UserProfileRepository  userProfileRepository;
    @Mock private CurrentUserProvider    currentUserProvider;
    @Mock private AnnouncementMapper     mapper;

    private AnnouncementService service;

    private static final UUID ANNOUNCEMENT_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID GOVERNING_BODY_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID USER_ID           = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID CITY_ID           = UUID.fromString("00000000-0000-0000-0000-000000000030");

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(
                announcementRepository, userProfileRepository, currentUserProvider, mapper);
        TenantContext.set(GOVERNING_BODY_ID);
        // lenient: not all tests call methods that need the current user (e.g. getPublicFeed)
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── getPublicFeed() ───────────────────────────────────────────────────────

    @Test
    void getPublicFeed_passesPUBLISHEDStatusToRepository_andReturnsCitizenDto() {
        Announcement published = makeAnnouncementWithStatus(AnnouncementStatus.PUBLISHED);
        published.setPublishedAt(Instant.now().minusSeconds(60));

        CitizenFeedResponse expected = makeCitizenFeedResponse("Water outage");

        when(announcementRepository.findFeedByGoverningBodyId(
                eq(GOVERNING_BODY_ID), any(), eq(AnnouncementStatus.PUBLISHED), any()))
                .thenReturn(List.of(published));
        when(mapper.toCitizenResponse(published)).thenReturn(expected);

        List<CitizenFeedResponse> result = service.getPublicFeed(null);

        assertThat(result).hasSize(1);
        // Returned type is CitizenFeedResponse — it has no governingBodyId field
        assertThat(result.get(0).title()).isEqualTo("Water outage");
        assertThat(result.get(0).publishedAt()).isNotNull();
    }

    @Test
    void getPublicFeed_noTenantAndNoWardId_returnsEmpty() {
        TenantContext.clear();  // citizen path — no platform JWT

        List<CitizenFeedResponse> result = service.getPublicFeed(null);

        assertThat(result).isEmpty();
        // Repository must not be called in this branch
    }

    @Test
    void getPublicFeed_withTenantAndWardId_callsWardScopedRepositoryQuery() {
        UUID wardId = UUID.fromString("00000000-0000-0000-0000-000000000040");
        Announcement published = makeAnnouncementWithStatus(AnnouncementStatus.PUBLISHED);
        published.setPublishedAt(Instant.now().minusSeconds(60));

        CitizenFeedResponse expected = makeCitizenFeedResponse("Ward notice");

        when(announcementRepository.findFeedByGoverningBodyIdAndWardId(
                eq(GOVERNING_BODY_ID), eq(wardId), any(), eq(AnnouncementStatus.PUBLISHED), any()))
                .thenReturn(List.of(published));
        when(mapper.toCitizenResponse(published)).thenReturn(expected);

        List<CitizenFeedResponse> result = service.getPublicFeed(wardId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Ward notice");
    }

    @Test
    void getPublicFeed_citizenPathWithWardId_callsGeographyQueryWithPublishedStatus() {
        TenantContext.clear();  // no operator context — community/citizen path
        UUID wardId = UUID.fromString("00000000-0000-0000-0000-000000000040");
        Announcement published = makeAnnouncementWithStatus(AnnouncementStatus.PUBLISHED);
        published.setPublishedAt(Instant.now().minusSeconds(60));

        CitizenFeedResponse expected = makeCitizenFeedResponse("Ward notice");

        when(announcementRepository.findFeedForCitizenByGeography(
                eq(wardId), eq(null), eq(null), eq(null),
                eq(AnnouncementStatus.PUBLISHED), any()))
                .thenReturn(List.of(published));
        when(mapper.toCitizenResponse(published)).thenReturn(expected);

        List<CitizenFeedResponse> result = service.getPublicFeed(wardId);

        assertThat(result).hasSize(1);
        // Confirm the DTO carries no operator fields (type-level guarantee — no governingBodyId field exists)
        assertThat(result.get(0)).isInstanceOf(CitizenFeedResponse.class);
    }

    // ── update() ─────────────────────────────────────────────────────────────

    @Test
    void update_draftAnnouncement_appliesMutableFields() {
        Announcement draft = makeDraftAnnouncement();
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(draft));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(null);

        AnnouncementUpdateRequest req = new AnnouncementUpdateRequest(
                "Updated title", "Updated content", "New summary",
                "ROADS", AnnouncementPriority.URGENT,
                null, true, "[\"road\"]");

        service.update(ANNOUNCEMENT_ID, req);

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        Announcement saved = captor.getValue();

        assertThat(saved.getTitle()).isEqualTo("Updated title");
        assertThat(saved.getContent()).isEqualTo("Updated content");
        assertThat(saved.getSummary()).isEqualTo("New summary");
        assertThat(saved.getCategory()).isEqualTo("ROADS");
        assertThat(saved.getPriority()).isEqualTo(AnnouncementPriority.URGENT);
        assertThat(saved.isPinned()).isTrue();
        assertThat(saved.getTags()).isEqualTo("[\"road\"]");
        assertThat(saved.getUpdatedByUserId()).isEqualTo(USER_ID);
    }

    @Test
    void update_nullFieldsPreserveExistingValues() {
        Announcement draft = makeDraftAnnouncement();
        draft.setTitle("Original title");
        draft.setPriority(AnnouncementPriority.NORMAL);

        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(draft));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(null);

        // All fields null — nothing should change except updatedByUserId
        AnnouncementUpdateRequest req = new AnnouncementUpdateRequest(
                null, null, null, null, null, null, null, null);

        service.update(ANNOUNCEMENT_ID, req);

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        Announcement saved = captor.getValue();

        assertThat(saved.getTitle()).isEqualTo("Original title");
        assertThat(saved.getPriority()).isEqualTo(AnnouncementPriority.NORMAL);
    }

    @Test
    void update_publishedAnnouncement_throws409() {
        Announcement published = makeAnnouncementWithStatus(AnnouncementStatus.PUBLISHED);
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(published));

        AnnouncementUpdateRequest req = new AnnouncementUpdateRequest(
                "title", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.update(ANNOUNCEMENT_ID, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void update_archivedAnnouncement_throws409() {
        Announcement archived = makeAnnouncementWithStatus(AnnouncementStatus.ARCHIVED);
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> service.update(ANNOUNCEMENT_ID,
                new AnnouncementUpdateRequest("t", null, null, null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void update_announcementNotFound_throwsNotFoundException() {
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ANNOUNCEMENT_ID,
                new AnnouncementUpdateRequest(null, null, null, null, null, null, null, null)))
                .isInstanceOf(AnnouncementNotFoundException.class);
    }

    @Test
    void update_futurePastExpiresAt_throws400() {
        Announcement draft = makeDraftAnnouncement();
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(draft));

        Instant past = Instant.now().minusSeconds(3600);
        AnnouncementUpdateRequest req = new AnnouncementUpdateRequest(
                null, null, null, null, null, past, null, null);

        assertThatThrownBy(() -> service.update(ANNOUNCEMENT_ID, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    // ── updateStatus() — DRAFT → PUBLISHED ───────────────────────────────────

    @Test
    void updateStatus_draftToPublished_setsPublishedAt() {
        Announcement draft = makeDraftAnnouncement();
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(draft));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(null);

        Instant before = Instant.now();
        service.updateStatus(ANNOUNCEMENT_ID, new AnnouncementStatusUpdateRequest(AnnouncementStatus.PUBLISHED));

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        Announcement saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);
        assertThat(saved.getPublishedAt()).isNotNull();
        assertThat(saved.getPublishedAt()).isAfterOrEqualTo(before);
        assertThat(saved.getUpdatedByUserId()).isEqualTo(USER_ID);
    }

    // ── updateStatus() — PUBLISHED → ARCHIVED ────────────────────────────────

    @Test
    void updateStatus_publishedToArchived_retainsPublishedAtAndSetsArchived() {
        Announcement published = makeAnnouncementWithStatus(AnnouncementStatus.PUBLISHED);
        Instant originalPublishedAt = Instant.now().minusSeconds(3600);
        published.setPublishedAt(originalPublishedAt);

        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(published));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(null);

        service.updateStatus(ANNOUNCEMENT_ID, new AnnouncementStatusUpdateRequest(AnnouncementStatus.ARCHIVED));

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        Announcement saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(AnnouncementStatus.ARCHIVED);
        assertThat(saved.getPublishedAt()).isEqualTo(originalPublishedAt);
    }

    // ── updateStatus() — invalid transitions ─────────────────────────────────

    @Test
    void updateStatus_publishedToDraft_throws409() {
        Announcement published = makeAnnouncementWithStatus(AnnouncementStatus.PUBLISHED);
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.updateStatus(ANNOUNCEMENT_ID,
                new AnnouncementStatusUpdateRequest(AnnouncementStatus.DRAFT)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void updateStatus_draftToArchived_throws409() {
        Announcement draft = makeDraftAnnouncement();
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.updateStatus(ANNOUNCEMENT_ID,
                new AnnouncementStatusUpdateRequest(AnnouncementStatus.ARCHIVED)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void updateStatus_archivedToAny_throws409() {
        Announcement archived = makeAnnouncementWithStatus(AnnouncementStatus.ARCHIVED);
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> service.updateStatus(ANNOUNCEMENT_ID,
                new AnnouncementStatusUpdateRequest(AnnouncementStatus.PUBLISHED)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void updateStatus_sameStatus_throws409() {
        Announcement published = makeAnnouncementWithStatus(AnnouncementStatus.PUBLISHED);
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.updateStatus(ANNOUNCEMENT_ID,
                new AnnouncementStatusUpdateRequest(AnnouncementStatus.PUBLISHED)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void updateStatus_targetExpired_throws400() {
        Announcement draft = makeDraftAnnouncement();
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.updateStatus(ANNOUNCEMENT_ID,
                new AnnouncementStatusUpdateRequest(AnnouncementStatus.EXPIRED)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void updateStatus_announcementNotFound_throwsNotFoundException() {
        when(announcementRepository.findByIdAndGoverningBodyId(ANNOUNCEMENT_ID, GOVERNING_BODY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(ANNOUNCEMENT_ID,
                new AnnouncementStatusUpdateRequest(AnnouncementStatus.PUBLISHED)))
                .isInstanceOf(AnnouncementNotFoundException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CitizenFeedResponse makeCitizenFeedResponse(String title) {
        return new CitizenFeedResponse(
                ANNOUNCEMENT_ID, title, "Body text.", "Short summary.",
                "WATER", AnnouncementPriority.IMPORTANT,
                AnnouncementTargetScope.CITY,
                Instant.now().minusSeconds(60), null, false, null, Instant.now());
    }

    private Announcement makeDraftAnnouncement() {
        return makeAnnouncementWithStatus(AnnouncementStatus.DRAFT);
    }

    private Announcement makeAnnouncementWithStatus(AnnouncementStatus status) {
        Announcement a = Announcement.builder()
                .title("Water outage")
                .content("Supply disrupted on 22 May.")
                .priority(AnnouncementPriority.IMPORTANT)
                .status(status)
                .governingBodyId(GOVERNING_BODY_ID)
                .targetScope(AnnouncementTargetScope.CITY)
                .cityId(CITY_ID)
                .build();
        ReflectionTestUtils.setField(a, "id", ANNOUNCEMENT_ID);
        return a;
    }
}
