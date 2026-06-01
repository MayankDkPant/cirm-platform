package com.cxp.platform.announcement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class AnnouncementNotFoundException extends ResponseStatusException {

    public AnnouncementNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Announcement not found: " + id);
    }
}
