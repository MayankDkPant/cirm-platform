package com.cxp.platform.announcement.service;

import com.cxp.platform.announcement.domain.Announcement;
import com.cxp.platform.announcement.domain.AnnouncementStatus;
import com.cxp.platform.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled sweep that transitions PUBLISHED announcements to EXPIRED when their
 * expiresAt timestamp has passed.
 *
 * Design invariants:
 *
 *   - Records are never deleted. Status transitions to EXPIRED are the only mutation.
 *   - One audit log line is emitted per expired announcement so each transition is
 *     independently traceable (id + expiresAt).
 *   - The worker never reads TenantContext — expiry is system-managed and crosses
 *     all tenant boundaries by design.
 *   - If two instances run simultaneously (future horizontal scaling), both may
 *     pick up the same batch. The outcome is idempotent: setting status = EXPIRED
 *     on an already-EXPIRED record is a no-op because findPublishedAndExpired
 *     filters on status = PUBLISHED. A SELECT FOR UPDATE lock is deferred to the
 *     horizontal-scaling phase, consistent with the Salesforce sync worker precedent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementExpiryWorker {

    private final AnnouncementRepository announcementRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireAnnouncements() {
        List<Announcement> candidates =
                announcementRepository.findPublishedAndExpired(AnnouncementStatus.PUBLISHED, Instant.now());

        if (candidates.isEmpty()) {
            return;
        }

        for (var announcement : candidates) {
            announcement.setStatus(AnnouncementStatus.EXPIRED);
            log.info("announcement_expired id={} expiresAt={}",
                    announcement.getId(), announcement.getExpiresAt());
        }

        announcementRepository.saveAll(candidates);

        log.info("announcement_expiry_sweep_completed count={}", candidates.size());
    }
}
