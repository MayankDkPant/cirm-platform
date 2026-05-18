package com.cxp.platform.complaint.application;

import com.cxp.platform.complaint.domain.Complaint;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class ExternalSyncBackoffPolicy {

    public boolean isReadyForRetry(Complaint complaint) {
        if (complaint.getExternalLastSyncAt() == null) {
            return true;
        }

        int attempts = complaint.getExternalSyncAttempts();
        if (attempts <= 0) {
            return true;
        }

        Duration requiredWait = switch (attempts) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            default -> Duration.ZERO;
        };

        if (requiredWait.isZero()) {
            return true;
        }

        Instant nextAllowedAt = complaint.getExternalLastSyncAt().plus(requiredWait);
        return !Instant.now().isBefore(nextAllowedAt);
    }
}
