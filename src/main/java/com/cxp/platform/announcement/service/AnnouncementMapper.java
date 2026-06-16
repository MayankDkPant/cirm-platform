package com.cxp.platform.announcement.service;

import com.cxp.platform.announcement.domain.Announcement;
import com.cxp.platform.announcement.dto.AnnouncementResponse;
import com.cxp.platform.announcement.dto.AttachmentRef;
import com.cxp.platform.announcement.dto.CitizenAnnouncementDetailResponse;
import com.cxp.platform.announcement.dto.CitizenFeedResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class AnnouncementMapper {

    AnnouncementResponse toResponse(Announcement a) {
        return new AnnouncementResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getSummary(),
                a.getCategory(),
                a.getPriority(),
                a.getStatus(),
                a.getGoverningBodyId(),
                a.getTargetScope(),
                a.getWardId(),
                a.getZoneId(),
                a.getCityId(),
                a.getDistrictId(),
                a.getStateId(),
                a.getPublishedAt(),
                a.getExpiresAt(),
                a.isPinned(),
                a.getTags(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    CitizenFeedResponse toCitizenResponse(Announcement a) {
        return new CitizenFeedResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getSummary(),
                a.getCategory(),
                a.getPriority(),
                a.getTargetScope(),
                a.getPublishedAt(),
                a.getExpiresAt(),
                a.isPinned(),
                a.getTags(),
                a.getCreatedAt()
        );
    }

    CitizenAnnouncementDetailResponse toPublicDetailResponse(Announcement a) {
        return new CitizenAnnouncementDetailResponse(
                a.getId(),
                a.getTitle(),
                a.getSummary(),
                a.getContent(),
                a.getCategory(),
                a.getPriority(),
                a.getTargetScope(),
                a.getPublishedAt(),
                a.getExpiresAt(),
                a.isPinned(),
                a.getTags(),
                a.getCreatedAt(),
                List.<AttachmentRef>of()
        );
    }
}
