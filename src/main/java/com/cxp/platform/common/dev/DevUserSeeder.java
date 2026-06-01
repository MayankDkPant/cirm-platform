package com.cxp.platform.common.dev;

import com.cxp.platform.auth.application.CitizenProvisioningService;
import com.cxp.platform.auth.web.dto.UserProvisionRequest;
import com.cxp.platform.auth.web.dto.UserProvisionResponse;
import com.cxp.platform.governance.domain.GoverningBody;
import com.cxp.platform.governance.repository.GoverningBodyRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seeds the local-dev user on startup via the real provisioning service.
 *
 * This replaces the raw-JDBC user creation that was previously buried inside
 * LocalDevContextFilter. Calling CitizenProvisioningService.provision() ensures
 * the dev user has both a users row AND a user_profile row, which means
 * GET /api/v1/users/me works correctly in local development.
 *
 * Caches userId and tenantId so DevIdentityFilter can inject auth context
 * on every unauthenticated request without hitting the database each time.
 *
 * Active only on the "local" Spring profile.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class DevUserSeeder implements ApplicationRunner {

    static final String DEV_USER_EMAIL = "dev-local@cxp.internal";

    private final CitizenProvisioningService citizenProvisioningService;
    private final GoverningBodyRepository governingBodyRepository;

    @Getter
    private volatile UUID devUserId;

    @Getter
    private volatile UUID devTenantId;

    @Override
    public void run(ApplicationArguments args) {
        try {
            UserProvisionResponse response = citizenProvisioningService.provision(
                    DEV_USER_EMAIL,
                    new UserProvisionRequest("Local Dev User", null, null, null, null, null, null)
            );
            this.devUserId = response.getId();

            this.devTenantId = governingBodyRepository.findFirstByIsActiveTrue()
                    .map(GoverningBody::getId)
                    .orElse(null);

            if (devTenantId == null) {
                log.warn("dev_user_seeder: no active governing_body found — " +
                        "dev requests will have no tenant context. Run Flyway migrations and seed data.");
            }

            log.warn("LOCAL DEV: dev user ready email={} userId={} tenantId={} onboardingComplete={}",
                    DEV_USER_EMAIL, devUserId, devTenantId, response.isOnboardingComplete());

        } catch (Exception ex) {
            log.error("dev_user_seeder: failed to seed dev user — dev auth injection will be unavailable. " +
                    "cause={}", ex.getMessage(), ex);
        }
    }
}
