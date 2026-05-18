package com.cxp.platform.complaint.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents complaint summary returned after successful submission.
 */
public record ComplaintResponse(
        UUID id,
        String status,
        String department,
        String priority,
        Instant createdAt
) {
}
