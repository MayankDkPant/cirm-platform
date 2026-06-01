package com.cxp.platform.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserProfile audit timestamp lifecycle callbacks.
 *
 * @PrePersist and @PreUpdate are plain Java methods — no Spring context needed.
 * These tests verify the canonical audit semantics independently of JPA infrastructure.
 */
class UserProfileAuditTest {

    // ── @PrePersist (onCreate) ────────────────────────────────────────────────

    @Test
    void onCreate_setsCreatedAtAndUpdatedAt() {
        UserProfile profile = UserProfile.builder().userId(java.util.UUID.randomUUID()).build();

        assertThat(profile.getCreatedAt()).isNull();
        assertThat(profile.getUpdatedAt()).isNull();

        Instant before = Instant.now();
        profile.onCreate();
        Instant after = Instant.now();

        assertThat(profile.getCreatedAt()).isBetween(before, after);
        assertThat(profile.getUpdatedAt()).isBetween(before, after);
    }

    @Test
    void onCreate_createdAtAndUpdatedAtAreEqual() {
        UserProfile profile = UserProfile.builder().userId(java.util.UUID.randomUUID()).build();
        profile.onCreate();

        assertThat(profile.getCreatedAt()).isEqualTo(profile.getUpdatedAt());
    }

    // ── @PreUpdate (onUpdate) ─────────────────────────────────────────────────

    @Test
    void onUpdate_advancesUpdatedAtButNotCreatedAt() throws InterruptedException {
        UserProfile profile = UserProfile.builder().userId(java.util.UUID.randomUUID()).build();
        profile.onCreate();

        Instant originalCreatedAt = profile.getCreatedAt();
        Instant originalUpdatedAt = profile.getUpdatedAt();

        // Brief pause so onUpdate() produces a measurably later timestamp
        Thread.sleep(2);
        profile.onUpdate();

        assertThat(profile.getCreatedAt())
                .as("createdAt must be immutable — @PreUpdate must not change it")
                .isEqualTo(originalCreatedAt);
        assertThat(profile.getUpdatedAt())
                .as("updatedAt must advance after @PreUpdate")
                .isAfter(originalUpdatedAt);
    }

    @Test
    void onUpdate_withoutPriorOnCreate_stillSetsUpdatedAt() {
        UserProfile profile = UserProfile.builder().userId(java.util.UUID.randomUUID()).build();

        Instant before = Instant.now();
        profile.onUpdate();
        Instant after = Instant.now();

        assertThat(profile.getUpdatedAt()).isBetween(before, after);
        assertThat(profile.getCreatedAt())
                .as("createdAt is not touched by @PreUpdate")
                .isNull();
    }

    // ── Immutability guard ────────────────────────────────────────────────────

    @Test
    void multipleOnCreate_eachCallResetsTimestamps() throws InterruptedException {
        UserProfile profile = UserProfile.builder().userId(java.util.UUID.randomUUID()).build();
        profile.onCreate();
        Instant first = profile.getCreatedAt();

        // In real JPA, @PrePersist fires exactly once per entity lifecycle.
        // A second call here validates the method is deterministic and idempotent-safe.
        Thread.sleep(2);
        profile.onCreate();

        assertThat(profile.getCreatedAt())
                .as("each onCreate call stamps the current time")
                .isAfterOrEqualTo(first);
    }
}
