package com.cxp.platform.announcement.service;

import com.cxp.platform.announcement.domain.Announcement;
import com.cxp.platform.announcement.dto.AnnouncementResponse;
import com.cxp.platform.announcement.dto.CitizenFeedResponse;
import org.springframework.stereotype.Component;

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
}
