package com.cxp.platform.auth.application;

import com.cxp.platform.auth.domain.User;
import com.cxp.platform.auth.domain.UserProfile;
import com.cxp.platform.auth.infrastructure.persistence.UserProfileRepository;
import com.cxp.platform.auth.infrastructure.persistence.UserRepository;
import com.cxp.platform.auth.web.dto.UserProvisionRequest;
import com.cxp.platform.auth.web.dto.UserProvisionResponse;
import com.cxp.platform.governance.domain.City;
import com.cxp.platform.governance.domain.District;
import com.cxp.platform.governance.domain.GoverningBody;
import com.cxp.platform.governance.domain.State;
import com.cxp.platform.governance.domain.Ward;
import com.cxp.platform.governance.repository.CityRepository;
import com.cxp.platform.governance.repository.DistrictRepository;
import com.cxp.platform.governance.repository.GoverningBodyRepository;
import com.cxp.platform.governance.repository.StateRepository;
import com.cxp.platform.governance.repository.WardRepository;
import com.cxp.platform.location.application.LocationIntelligenceService;
import com.cxp.platform.location.domain.LocationRoutingRequest;
import com.cxp.platform.location.domain.LocationRoutingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CitizenProvisioningService — focuses on the new governance routing paths:
 *
 *   Path A — GPS → ward-locator → routing (existing behavior, regression guard)
 *   Path B — Explicit wardId from discovery hierarchy → validated → hierarchy derived
 *   Path C — Neither GPS nor wardId → personal fields only (existing behavior, regression guard)
 *
 * Uses Mockito only — no Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class CitizenProvisioningServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private LocationIntelligenceService locationIntelligenceService;
    @Mock private WardRepository wardRepository;
    @Mock private GoverningBodyRepository governingBodyRepository;
    @Mock private CityRepository cityRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private StateRepository stateRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private CitizenProvisioningService service;

    private static final String EMAIL = "citizen@example.com";
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID WARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID GOVERNING_BODY_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID CITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID DISTRICT_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");
    private static final UUID STATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000050");

    @BeforeEach
    void setUp() {
        service = new CitizenProvisioningService(
                userRepository, userProfileRepository, locationIntelligenceService,
                wardRepository, governingBodyRepository, cityRepository,
                districtRepository, stateRepository, currentUserProvider);
    }

    // ── Path A — GPS ──────────────────────────────────────────────────────────

    @Test
    void gpsPath_wardResolvedFromCoordinates_fullHierarchyPopulated() {
        User user = makeUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // GPS resolves a ward with governing body
        when(locationIntelligenceService.resolveRouting(any(LocationRoutingRequest.class)))
                .thenReturn(LocationRoutingResult.builder()
                        .wardId(WARD_ID)
                        .wardName("Dalanwala")
                        .governingBodyId(GOVERNING_BODY_ID)
                        .resolvedLatitude(30.3165)
                        .resolvedLongitude(78.0322)
                        .build());

        GoverningBody gb = makeGoverningBody();
        when(governingBodyRepository.findById(GOVERNING_BODY_ID)).thenReturn(Optional.of(gb));
        when(cityRepository.findById(CITY_ID)).thenReturn(Optional.of(makeCity("Dehradun")));
        when(districtRepository.findById(DISTRICT_ID)).thenReturn(Optional.of(makeDistrict("Dehradun District")));
        when(stateRepository.findById(STATE_ID)).thenReturn(Optional.of(makeState("Uttarakhand")));
        when(wardRepository.findById(WARD_ID)).thenReturn(Optional.of(makeWard()));

        UserProvisionRequest req = new UserProvisionRequest("Ravi Kumar", "9876543210",
                "12 Main Road", 30.3165, 78.0322, "248001", null);

        UserProvisionResponse resp = service.provision(EMAIL, req);

        assertThat(resp.getWardId()).isEqualTo(WARD_ID.toString());
        assertThat(resp.getWardName()).isEqualTo("Dalanwala");
        assertThat(resp.getGoverningBodyId()).isEqualTo(GOVERNING_BODY_ID.toString());
        assertThat(resp.getCityId()).isEqualTo(CITY_ID.toString());
        assertThat(resp.getCity()).isEqualTo("Dehradun");
        assertThat(resp.getDistrictId()).isEqualTo(DISTRICT_ID.toString());
        assertThat(resp.getDistrict()).isEqualTo("Dehradun District");
        assertThat(resp.getStateId()).isEqualTo(STATE_ID.toString());
        assertThat(resp.getState()).isEqualTo("Uttarakhand");
        assertThat(resp.isOnboardingComplete()).isTrue();
    }

    @Test
    void gpsPath_wardResolved_explicitWardIdInBodyIsIgnored() {
        User user = makeUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID differentWardId = UUID.fromString("00000000-0000-0000-0000-0000000000FF");
        when(locationIntelligenceService.resolveRouting(any()))
                .thenReturn(LocationRoutingResult.builder()
                        .wardId(WARD_ID)
                        .wardName("Dalanwala")
                        .governingBodyId(GOVERNING_BODY_ID)
                        .build());

        when(governingBodyRepository.findById(GOVERNING_BODY_ID)).thenReturn(Optional.of(makeGoverningBody()));
        when(cityRepository.findById(CITY_ID)).thenReturn(Optional.empty());
        when(districtRepository.findById(DISTRICT_ID)).thenReturn(Optional.empty());
        when(stateRepository.findById(STATE_ID)).thenReturn(Optional.empty());
        when(wardRepository.findById(WARD_ID)).thenReturn(Optional.of(makeWard()));

        // Body contains a different wardId — GPS ward must win
        UserProvisionRequest req = new UserProvisionRequest("Ravi Kumar", null,
                null, 30.3165, 78.0322, null, differentWardId);

        UserProvisionResponse resp = service.provision(EMAIL, req);

        assertThat(resp.getWardId()).isEqualTo(WARD_ID.toString());
    }

    // ── Path B — Explicit wardId ──────────────────────────────────────────────

    @Test
    void explicitWardPath_wardFoundInDb_fullHierarchyDerivedFromFK() {
        User user = makeUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // GPS returns empty (no coordinates)
        when(locationIntelligenceService.resolveRouting(any()))
                .thenReturn(LocationRoutingResult.builder().build());

        // Ward present in DB with governing body FK
        when(wardRepository.findById(WARD_ID)).thenReturn(Optional.of(makeWard()));

        GoverningBody gb = makeGoverningBody();
        when(governingBodyRepository.findById(GOVERNING_BODY_ID)).thenReturn(Optional.of(gb));
        when(cityRepository.findById(CITY_ID)).thenReturn(Optional.of(makeCity("Dehradun")));
        when(districtRepository.findById(DISTRICT_ID)).thenReturn(Optional.of(makeDistrict("Dehradun District")));
        when(stateRepository.findById(STATE_ID)).thenReturn(Optional.of(makeState("Uttarakhand")));

        UserProvisionRequest req = new UserProvisionRequest("Ravi Kumar", "9876543210",
                "12 Main Road", null, null, "248001", WARD_ID);

        UserProvisionResponse resp = service.provision(EMAIL, req);

        assertThat(resp.getWardId()).isEqualTo(WARD_ID.toString());
        assertThat(resp.getWardName()).isEqualTo("Balawala");
        assertThat(resp.getGoverningBodyId()).isEqualTo(GOVERNING_BODY_ID.toString());
        assertThat(resp.getCityId()).isEqualTo(CITY_ID.toString());
        assertThat(resp.getCity()).isEqualTo("Dehradun");
        assertThat(resp.getDistrictId()).isEqualTo(DISTRICT_ID.toString());
        assertThat(resp.getDistrict()).isEqualTo("Dehradun District");
        assertThat(resp.getStateId()).isEqualTo(STATE_ID.toString());
        assertThat(resp.getState()).isEqualTo("Uttarakhand");
        assertThat(resp.isOnboardingComplete()).isTrue();
    }

    @Test
    void explicitWardPath_unknownWardId_throwsIllegalArgument() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(makeUser()));

        // GPS returns empty (no coordinates)
        when(locationIntelligenceService.resolveRouting(any()))
                .thenReturn(LocationRoutingResult.builder().build());

        // Ward NOT in DB
        UUID unknownWardId = UUID.fromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF");
        when(wardRepository.findById(unknownWardId)).thenReturn(Optional.empty());

        UserProvisionRequest req = new UserProvisionRequest(null, null,
                null, null, null, null, unknownWardId);

        assertThatThrownBy(() -> service.provision(EMAIL, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(unknownWardId.toString());
    }

    // ── Concurrent provisioning race ─────────────────────────────────────────

    @Test
    void concurrentProvision_duplicateEmailInsert_recoversViaReQuery() {
        // Simulate the TOCTOU window: findByEmail returns empty on first call (pre-insert),
        // save throws DataIntegrityViolationException (concurrent insert won the race),
        // second findByEmail returns the concurrently-created user → safe recovery, no 500.
        User concurrentlyCreatedUser = makeUser();
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty())           // first call: not found yet
                .thenReturn(Optional.of(concurrentlyCreatedUser)); // re-query after constraint violation
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uq_users_email"));

        when(locationIntelligenceService.resolveRouting(any()))
                .thenReturn(LocationRoutingResult.builder().build());

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProvisionRequest req = new UserProvisionRequest("Ravi Kumar", null,
                null, null, null, null, null);

        // Must succeed and return the concurrently-created user, not throw a 500
        UserProvisionResponse resp = service.provision(EMAIL, req);
        assertThat(resp.getId()).isEqualTo(USER_ID);
    }

    // ── Path C — no GPS, no wardId ────────────────────────────────────────────

    @Test
    void noGpsNoWardId_onlyPersonalFieldsUpdated_existingGeographyPreserved() {
        User user = makeUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // Existing profile with ward already resolved
        UserProfile existingProfile = UserProfile.builder()
                .userId(USER_ID)
                .wardUuid(WARD_ID)
                .wardName("Dalanwala")
                .governingBodyUuid(GOVERNING_BODY_ID)
                .cityUuid(CITY_ID)
                .districtUuid(DISTRICT_ID)
                .stateUuid(STATE_ID)
                .build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // GPS returns empty
        when(locationIntelligenceService.resolveRouting(any()))
                .thenReturn(LocationRoutingResult.builder().build());

        // No hierarchy names lookup needed since routing is empty — but getMyProfile style
        // uses profile's stored IDs; provision() here returns empty names (routing empty, no gb)
        UserProvisionRequest req = new UserProvisionRequest("New Name", null,
                null, null, null, null, null);

        UserProvisionResponse resp = service.provision(EMAIL, req);

        // Ward from existing profile is preserved — onboarding stays complete
        assertThat(resp.isOnboardingComplete()).isTrue();
        // Governance IDs from routing are null (no routing resolved), but profile keeps them
        // The response wardId comes from routing (empty in this case)
        assertThat(resp.getWardId()).isNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User makeUser() {
        User u = User.builder()
                .email(EMAIL)
                .role(User.Role.CITIZEN)
                .enabled(true)
                .build();
        // BaseEntity.id has no setter — use reflection to set it in tests
        ReflectionTestUtils.setField(u, "id", USER_ID);
        return u;
    }

    private Ward makeWard() {
        GoverningBody gb = makeGoverningBody();
        Ward w = new Ward();
        w.setId(WARD_ID);
        w.setName("Balawala");
        w.setGoverningBody(gb);
        return w;
    }

    private GoverningBody makeGoverningBody() {
        GoverningBody gb = new GoverningBody();
        gb.setId(GOVERNING_BODY_ID);
        gb.setName("Dehradun Municipal Corporation");
        gb.setCityId(CITY_ID);
        gb.setDistrictId(DISTRICT_ID);
        gb.setStateId(STATE_ID);
        gb.setActive(true);
        return gb;
    }

    private City makeCity(String name) {
        City c = new City();
        c.setId(CITY_ID);
        c.setName(name);
        return c;
    }

    private District makeDistrict(String name) {
        District d = new District();
        d.setId(DISTRICT_ID);
        d.setName(name);
        d.setStateId(STATE_ID);
        return d;
    }

    private State makeState(String name) {
        State s = new State();
        s.setId(STATE_ID);
        s.setName(name);
        return s;
    }
}
