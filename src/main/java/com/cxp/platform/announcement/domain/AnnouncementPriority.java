package com.cxp.platform.announcement.domain;

public enum AnnouncementPriority {

    /** Routine information — no urgency signal to citizens. */
    NORMAL,

    /** Notable civic matter — highlighted in the feed. */
    IMPORTANT,

    /** Time-sensitive — triggers push notification routing when enabled. */
    URGENT,

    /** Emergency — highest routing priority across all channels. */
    CRITICAL
}
