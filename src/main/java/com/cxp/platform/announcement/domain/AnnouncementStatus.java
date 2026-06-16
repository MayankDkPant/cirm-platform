package com.cxp.platform.announcement.domain;

public enum AnnouncementStatus {

    /** Created but not yet visible to citizens. Operator can edit freely. */
    DRAFT,

    /** Live — visible to the targeted audience per visibility_scope. */
    PUBLISHED,

    /** Manually withdrawn from the civic feed. Retained for audit. */
    ARCHIVED,

    /** System-set when expires_at passes and a sweep job transitions the row. */
    EXPIRED
}
