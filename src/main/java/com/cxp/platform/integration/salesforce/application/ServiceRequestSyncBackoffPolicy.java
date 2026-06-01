package com.cxp.platform.integration.salesforce.application;

import com.cxp.platform.servicerequest.entity.ServiceRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class ServiceRequestSyncBackoffPolicy {

    public boolean isReadyForRetry(ServiceRequest request) {
        if (request.getExternalSyncLastAttemptAt() == null) {
            return true;
        }

        int attempts = request.getExternalSyncAttempts();
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

        Instant nextAllowedAt = request.getExternalSyncLastAttemptAt().plus(requiredWait);
        return !Instant.now().isBefore(nextAllowedAt);
    }
}
