package com.cxp.platform.servicerequest.entity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ServiceRequestEvent audit and immutability invariants.
 *
 * ServiceRequestEvent is append-only audit history — records must never be
 * modified after creation. These tests verify:
 *   1. @PrePersist stamps createdAt automatically.
 *   2. The class exposes no public setters — immutability is enforced at the
 *      Java layer, complementing the ON DELETE RESTRICT FK added in V52.
 *
 * No Spring context needed — callbacks and reflection work on plain instances.
 */
class ServiceRequestEventAuditTest {

    // ── @PrePersist (onCreate) ────────────────────────────────────────────────

    @Test
    void onCreate_setsCreatedAt() {
        ServiceRequestEvent event = ServiceRequestEvent.builder()
                .serviceRequestId(UUID.randomUUID())
                .eventType("CREATED")
                .build();

        assertThat(event.getCreatedAt()).isNull();

        Instant before = Instant.now();
        event.onCreate();
        Instant after = Instant.now();

        assertThat(event.getCreatedAt()).isBetween(before, after);
    }

    @Test
    void onCreate_calledTwice_updatesCreatedAt() throws InterruptedException {
        ServiceRequestEvent event = ServiceRequestEvent.builder()
                .serviceRequestId(UUID.randomUUID())
                .eventType("CREATED")
                .build();
        event.onCreate();
        Instant first = event.getCreatedAt();

        Thread.sleep(2);
        event.onCreate();

        assertThat(event.getCreatedAt())
                .as("each onCreate call stamps current time")
                .isAfterOrEqualTo(first);
    }

    // ── Immutability guard ────────────────────────────────────────────────────

    @Test
    void noPublicSetters_enforceImmutabilityAtJavaLayer() {
        List<String> publicSetters = Arrays.stream(ServiceRequestEvent.class.getMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();

        assertThat(publicSetters)
                .as("ServiceRequestEvent must have no public setters — it is append-only audit history. "
                        + "Found: " + publicSetters)
                .isEmpty();
    }
}
