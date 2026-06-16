package com.cxp.platform.announcement.service;

import com.cxp.platform.announcement.domain.Announcement;
import com.cxp.platform.announcement.domain.AnnouncementStatus;
import com.cxp.platform.announcement.repository.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static com.cxp.platform.announcement.domain.AnnouncementStatus.EXPIRED;
import static com.cxp.platform.announcement.domain.AnnouncementStatus.PUBLISHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnnouncementExpiryWorker.
 *
 * Repository filtering (PUBLISHED + isActive + expiresAt < now) is the contract of the
 * query method and is verified separately. These tests exercise the worker's own logic:
 *
 *   1. All returned candidates are transitioned to EXPIRED.
 *   2. saveAll is called with every candidate after mutation.
 *   3. saveAll is NOT called when no candidates are returned (no-op sweep).
 *   4. Multiple candidates in one sweep are all transitioned.
 *
 * No Spring context, no DB, no Flyway.
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementExpiryWorkerTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AnnouncementExpiryWorker worker;

    @Captor
    private ArgumentCaptor<List<Announcement>> savedCaptor;

    // ── Core transition ───────────────────────────────────────────────────────

    @Test
    void sweep_singleExpiredAnnouncement_transitionsStatusToExpired() {
        Announcement announcement = publishedExpired();
        when(announcementRepository.findPublishedAndExpired(eq(PUBLISHED), any(Instant.class)))
                .thenReturn(List.of(announcement));

        worker.expireAnnouncements();

        assertThat(announcement.getStatus()).isEqualTo(EXPIRED);
    }

    @Test
    void sweep_singleExpiredAnnouncement_persistsWithSaveAll() {
        Announcement announcement = publishedExpired();
        when(announcementRepository.findPublishedAndExpired(eq(PUBLISHED), any(Instant.class)))
                .thenReturn(List.of(announcement));

        worker.expireAnnouncements();

        verify(announcementRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue())
                .hasSize(1)
                .extracting(Announcement::getStatus)
                .containsOnly(EXPIRED);
    }

    @Test
    void sweep_multipleExpiredAnnouncements_allTransitioned() {
        Announcement a1 = publishedExpired();
        Announcement a2 = publishedExpired();
        when(announcementRepository.findPublishedAndExpired(eq(PUBLISHED), any(Instant.class)))
                .thenReturn(List.of(a1, a2));

        worker.expireAnnouncements();

        verify(announcementRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue())
                .hasSize(2)
                .extracting(Announcement::getStatus)
                .containsOnly(EXPIRED);
    }

    // ── No-op sweep ───────────────────────────────────────────────────────────

    @Test
    void sweep_noExpiredAnnouncements_doesNotCallSaveAll() {
        when(announcementRepository.findPublishedAndExpired(any(), any())).thenReturn(List.of());

        worker.expireAnnouncements();

        verify(announcementRepository, never()).saveAll(any());
    }

    // ── Query contract ────────────────────────────────────────────────────────

    @Test
    void sweep_queriesWithPublishedStatusOnly() {
        when(announcementRepository.findPublishedAndExpired(any(), any())).thenReturn(List.of());

        worker.expireAnnouncements();

        // Worker must never query with DRAFT, ARCHIVED, or EXPIRED — those must never
        // be returned by findPublishedAndExpired, and the worker must not request them.
        verify(announcementRepository).findPublishedAndExpired(eq(PUBLISHED), any(Instant.class));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Announcement publishedExpired() {
        return Announcement.builder()
                .status(AnnouncementStatus.PUBLISHED)
                .expiresAt(Instant.now().minusSeconds(3_600))
                .build();
    }
}
