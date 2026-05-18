package com.cirm.platform.api.error;

import java.time.LocalDateTime;

/**
 * Standard API error response used across the entire platform.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}
