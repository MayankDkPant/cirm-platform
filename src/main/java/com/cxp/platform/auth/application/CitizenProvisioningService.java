package com.cxp.platform.auth.application;

import com.cxp.platform.auth.domain.User;
import com.cxp.platform.auth.domain.UserProfile;
import java.math.BigDecimal;
import com.cxp.platform.auth.infrastructure.persistence.UserProfileRepository;
import com.cxp.platform.auth.infrastructure.persistence.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import com.cxp.platform.auth.web.dto.CitizenProfileResponse;
import com.cxp.platform.auth.web.dto.UserProvisionRequest;
import com.cxp.platform.auth.web.dto.UserProvisionResponse;
import com.cxp.platform.governance.domain.GoverningBody;
import com.cxp.platform.governance.domain.Ward;
import com.cxp.platform.governance.repository.CityRepository;
import com.cxp.platform.governance.repository.DistrictRepository;
import com.cxp.platform.governance.repository.GoverningBodyRepository;
import com.cxp.platform.governance.repository.StateRepository;
import com.cxp.platform.governance.repository.WardRepository;
import com.cxp.platform.location.application.LocationIntelligenceService;
import com.cxp.platform.location.domain.LocationRoutingRequest;
import com.cxp.platform.location.domain.LocationRoutingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates citizen onboarding and profile maintenance.
 *
 * Responsibilities:
 *   - Find or create the User identity record for a given email.
 *   - Resolve civic geography via one of two paths (GPS takes precedence):
 *       Path A — GPS: coordinates → ward-locator → ward → governingBody → hierarchy names.
 *       Path B — Explicit ward: wardId from discovery hierarchy → ward validated → hierarchy derived.
 *   - Walk the governance hierarchy (ward → governingBody → city/district/state) to populate names.
 *   - Create or update UserProfile idempotently.
 *
 * Discovery APIs are the authoritative source for governance IDs:
 *   GET /states → /districts → /cities → /governing-bodies → /wards → /departments
 *
 * This service is temporarily located in the auth module because the User entity
 * and UserProfile entity share the same persistence boundary. It is a candidate
 * for extraction into a dedicated citizen/identity module once that module boundary
 * is formally established.
 *
 * Auth vs. Civic Identity boundary:
 *   auth module   → User, JWT, OTP, session lifecycle
 *   this service  → UserProfile, civic geography, onboarding state (civic identity)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenProvisioningService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final LocationIntelligenceService locationIntelligenceService;
    private final WardRepository wardRepository;
    private final GoverningBodyRepository governingBodyRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final StateRepository stateRepository;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates or updates the citizen's User + UserProfile. Fully idempotent.
     *
     * CONSISTENCY MODEL: @Transactional wraps the entire method, but the ward-locator
     * HTTP call (step 2) occurs inside the transaction boundary when GPS coordinates are
     * provided. The DB connection is held open for the duration of the external call.
     * Ward-locator failures are handled gracefully (empty result returned), so they do
     * not trigger a rollback — but they DO extend transaction duration under load.
     *
     * FUTURE FIX: extract resolveRouting() outside the @Transactional boundary so the
     * DB connection is acquired only for the actual read/write phase. Deferred.
     *
     * ATOMICITY INVARIANT: User creation + UserProfile creation/update must succeed
     * together. Both are inside this @Transactional. findOrCreateUser handles the
     * concurrent-insert race via DataIntegrityViolationException re-query.
     *
     * AUTHORITY: governingBodyId is derived from the Ward → GoverningBody FK in the
     * local DB — authoritative. Ward-locator returns only a geographic hint (wardName);
     * the UUID and governing body are resolved from the local WardRepository.
     *
     * @param verifiedEmail email extracted from the cryptographically validated JWT — never from
     *                      the request body, which is untrusted client input.
     *
     * Case A — GPS coordinates present: resolves ward via ward-locator service → hierarchy.
     *          wardId in the request body is ignored when GPS successfully resolves a ward.
     * Case B — Explicit wardId, no GPS: wardId must be a valid UUID from
     *          GET /governing-bodies/{id}/wards. Hierarchy is derived from the ward FK chain.
     * Case C — Neither GPS nor wardId: preserves existing geography; updates personal fields only.
     */
    @Transactional
    public UserProvisionResponse provision(String verifiedEmail, UserProvisionRequest req) {
        log.info("USER_PROVISION_SERVICE_START verifiedEmail={}", verifiedEmail);
        try {
            User user = findOrCreateUser(verifiedEmail);

            LocationRoutingResult routing = resolveRouting(req);
            HierarchyNames names = resolveHierarchyNames(routing);

            UserProfile profile = userProfileRepository.findByUserId(user.getId())
                    .orElseGet(() -> UserProfile.builder().userId(user.getId()).build());

            applyProfileUpdates(profile, req, routing, names);
            userProfileRepository.save(profile);

            log.info("USER_PROVISION_SUCCESS userId={} verifiedEmail={} wardId={} governingBodyId={} onboardingComplete={}",
                    user.getId(), verifiedEmail, routing.getWardId(), routing.getGoverningBodyId(), profile.hasWard());

            return UserProvisionResponse.builder()
                    .id(user.getId())
                    .wardId(routing.getWardId()             != null ? routing.getWardId().toString()             : null)
                    .wardName(routing.getWardName())
                    .zoneId(profile.getZoneUuid()           != null ? profile.getZoneUuid().toString()           : null)
                    .governingBodyId(routing.getGoverningBodyId() != null ? routing.getGoverningBodyId().toString() : null)
                    .cityId(names.cityId()                  != null ? names.cityId().toString()                  : null)
                    .city(names.cityName())
                    .districtId(names.districtId()          != null ? names.districtId().toString()              : null)
                    .district(names.districtName())
                    .stateId(names.stateId()                != null ? names.stateId().toString()                 : null)
                    .state(names.stateName())
                    .onboardingComplete(profile.hasWard())
                    .build();
        } catch (Exception ex) {
            log.error("USER_PROVISION_FAILED verifiedEmail={} type={} message={}",
                    verifiedEmail, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Returns the authenticated citizen's full civic profile for GET /me.
     */
    @Transactional(readOnly = true)
    public CitizenProfileResponse getMyProfile() {
        UUID userId = currentUserProvider.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "No civic profile for userId=" + userId + ". Call POST /api/v1/users/provision first."));

        HierarchyNames names = resolveNamesFromProfile(profile);

        log.debug("PROFILE_ADDRESS_MAPPED userId={} addressNormalized={}", userId, profile.getAddressNormalized());

        return CitizenProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .memberSince(user.getCreatedAt())
                .fullName(profile.getFullName())
                .mobileNumber(profile.getPhoneNumber())
                .addressLine(profile.getAddressNormalized())
                .pinCode(profile.getPostalCode())
                // Canonical governance IDs — authoritative references into the discovery hierarchy
                .wardId(profile.getWardUuid()           != null ? profile.getWardUuid().toString()           : null)
                .wardName(profile.getWardName())
                .governingBodyId(profile.getGoverningBodyUuid() != null ? profile.getGoverningBodyUuid().toString() : null)
                .cityId(profile.getCityUuid()           != null ? profile.getCityUuid().toString()           : null)
                .districtId(profile.getDistrictUuid()   != null ? profile.getDistrictUuid().toString()       : null)
                .stateId(profile.getStateUuid()         != null ? profile.getStateUuid().toString()          : null)
                // Derived display labels — convenience, not canonical
                .city(names.cityName())
                .district(names.districtName())
                .state(names.stateName())
                .latitude(profile.getLatitude()   != null ? profile.getLatitude().doubleValue()  : null)
                .longitude(profile.getLongitude() != null ? profile.getLongitude().doubleValue() : null)
                .isVerified(profile.isPhoneVerified() || profile.isEmailVerified())
                .onboardingComplete(profile.hasWard())
                // locality: no DB column yet — deferred until address normalization is extended
                .locality(null)
                // profileImageUrl: no media storage yet — deferred until media service is implemented
                .profileImageUrl(null)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Idempotent user lookup-or-create.
     *
     * The try/catch handles the rare concurrent race where two onboarding requests for the
     * same email both read "not found" and both attempt the INSERT simultaneously. The second
     * INSERT fails on the uq_users_email UNIQUE constraint; we re-query rather than propagating
     * a 500 — the second caller gets the same User the first caller just created.
     */
    private User findOrCreateUser(String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            try {
                return userRepository.save(User.builder()
                        .email(email)
                        .role(User.Role.CITIZEN)
                        .enabled(true)
                        .build());
            } catch (DataIntegrityViolationException ex) {
                return userRepository.findByEmail(email)
                        .orElseThrow(() -> new IllegalStateException(
                                "User not found after concurrent insert for email=" + email, ex));
            }
        });
    }

    /**
     * Routing resolution — GPS takes precedence (Path A); falls back to explicit wardId (Path B).
     *
     * Path A: GPS coordinates → ward-locator service → routing with governingBodyId derived
     *         from the Ward → GoverningBody FK (authoritative).
     * Path B: explicit wardId → ward validated in DB → routing synthesised from the ward entity.
     *         Throws IllegalArgumentException (→ 400) if wardId is unknown.
     * Path C: neither → empty routing; only personal fields are updated.
     */
    private LocationRoutingResult resolveRouting(UserProvisionRequest req) {
        LocationRoutingResult gpsRouting = locationIntelligenceService.resolveRouting(
                LocationRoutingRequest.builder()
                        .latitude(req.latitude())
                        .longitude(req.longitude())
                        .build()
        );
        if (gpsRouting.getWardId() != null) {
            return gpsRouting;
        }
        if (req.wardId() != null) {
            return resolveRoutingFromExplicitWard(req.wardId());
        }
        return gpsRouting;
    }

    /**
     * Validates and resolves routing from a citizen-selected wardId.
     *
     * The wardId MUST come from GET /governing-bodies/{id}/wards — the discovery API is the
     * only authoritative source. Callers must never invent or hard-code ward UUIDs.
     *
     * Throws IllegalArgumentException if the wardId is not present in the DB.
     * The governing body is derived from the Ward → GoverningBody FK, making it
     * authoritative and immune to client-supplied hierarchy inconsistencies.
     */
    private LocationRoutingResult resolveRoutingFromExplicitWard(UUID wardId) {
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "wardId " + wardId + " not found. "
                        + "Use GET /api/v1/governing-bodies/{id}/wards to browse valid wards."));
        GoverningBody governingBody = ward.getGoverningBody();
        return LocationRoutingResult.builder()
                .wardId(ward.getId())
                .wardName(ward.getName())
                .governingBodyId(governingBody.getId())
                .municipalityId(governingBody.getId())
                .build();
    }

    private void applyProfileUpdates(UserProfile profile, UserProvisionRequest req,
                                     LocationRoutingResult routing, HierarchyNames names) {
        if (req.fullName() != null)     profile.setFullName(req.fullName());
        if (req.mobileNumber() != null) profile.setPhoneNumber(req.mobileNumber());
        if (req.pinCode() != null)      profile.setPostalCode(req.pinCode());

        if (routing.getResolvedLatitude()  != null) profile.setLatitude(BigDecimal.valueOf(routing.getResolvedLatitude()));
        if (routing.getResolvedLongitude() != null) profile.setLongitude(BigDecimal.valueOf(routing.getResolvedLongitude()));
        // addressNormalized stores the citizen's raw address input — not geocoded output.
        // The field name is aspirational; a geocoding pass will replace this with normalized output.
        if (req.address() != null && !req.address().isBlank()) profile.setAddressNormalized(req.address());

        if (routing.getWardId() != null) {
            profile.setWardUuid(routing.getWardId());
            profile.setWardName(routing.getWardName());
            wardRepository.findById(routing.getWardId())
                    .map(Ward::getZoneId)
                    .ifPresent(profile::setZoneUuid);
        }

        // Use governingBodyId (FK-derived, authoritative) not municipalityId (external-service value)
        if (routing.getGoverningBodyId() != null) profile.setGoverningBodyUuid(routing.getGoverningBodyId());

        if (names.cityId()     != null) profile.setCityUuid(names.cityId());
        if (names.districtId() != null) profile.setDistrictUuid(names.districtId());
        if (names.stateId()    != null) profile.setStateUuid(names.stateId());
    }

    private HierarchyNames resolveHierarchyNames(LocationRoutingResult routing) {
        if (routing.getGoverningBodyId() == null) return HierarchyNames.empty();
        return governingBodyRepository.findById(routing.getGoverningBodyId())
                .map(this::namesFromGoverningBody)
                .orElse(HierarchyNames.empty());
    }

    private HierarchyNames resolveNamesFromProfile(UserProfile profile) {
        return new HierarchyNames(
                profile.getCityUuid(),     lookupCityName(profile.getCityUuid()),
                profile.getDistrictUuid(), lookupDistrictName(profile.getDistrictUuid()),
                profile.getStateUuid(),    lookupStateName(profile.getStateUuid()));
    }

    private HierarchyNames namesFromGoverningBody(GoverningBody gb) {
        return new HierarchyNames(
                gb.getCityId(),     lookupCityName(gb.getCityId()),
                gb.getDistrictId(), lookupDistrictName(gb.getDistrictId()),
                gb.getStateId(),    lookupStateName(gb.getStateId()));
    }

    private String lookupCityName(UUID id) {
        return id == null ? null : cityRepository.findById(id).map(c -> c.getName()).orElse(null);
    }

    private String lookupDistrictName(UUID id) {
        return id == null ? null : districtRepository.findById(id).map(d -> d.getName()).orElse(null);
    }

    private String lookupStateName(UUID id) {
        return id == null ? null : stateRepository.findById(id).map(s -> s.getName()).orElse(null);
    }

    private record HierarchyNames(
            UUID cityId, String cityName,
            UUID districtId, String districtName,
            UUID stateId, String stateName
    ) {
        static HierarchyNames empty() {
            return new HierarchyNames(null, null, null, null, null, null);
        }
    }
}
