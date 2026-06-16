package com.cxp.platform.location;

import com.cxp.platform.governance.repository.WardRepository;
import com.cxp.platform.location.application.DefaultLocationIntelligenceService;
import com.cxp.platform.location.application.WardLookupService;
import com.cxp.platform.location.domain.LocationRoutingRequest;
import com.cxp.platform.location.domain.LocationRoutingResult;
import com.cxp.platform.location.domain.WardLookupResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DefaultLocationIntelligenceService — verifies geo field semantics
 * documented in the PR4 geo governance audit:
 *
 *   1. No coordinates → empty result (all nulls)
 *   2. Coordinates provided → GPS echoed back in resolvedLatitude / resolvedLongitude
 *   3. formattedAddress is always null (ward-locator returns wardNo + wardName only)
 *   4. governingBodyId is derived from local DB, not from ward-locator municipalityId
 *   5. Ward-locator returns null → wardId and governingBodyId are null
 */
@ExtendWith(MockitoExtension.class)
class DefaultLocationIntelligenceServiceTest {

    @Mock private WardLookupService wardLookupService;
    @Mock private WardRepository wardRepository;

    private DefaultLocationIntelligenceService service;

    private static final double LAT = 30.3165;
    private static final double LON = 78.0322;
    private static final UUID WARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID GOVERNING_BODY_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @BeforeEach
    void setUp() {
        service = new DefaultLocationIntelligenceService(
                Optional.of(wardLookupService),
                wardRepository
        );
    }

    // ── No coordinates ────────────────────────────────────────────────────────

    @Test
    void noCoordinates_returnsEmptyResult() {
        LocationRoutingResult result = service.resolveRouting(
                LocationRoutingRequest.builder().latitude(null).longitude(null).build());

        assertThat(result.getWardId()).isNull();
        assertThat(result.getGoverningBodyId()).isNull();
        assertThat(result.getWardName()).isNull();
        assertThat(result.getFormattedAddress()).isNull();
        assertThat(result.getResolvedLatitude()).isNull();
        assertThat(result.getResolvedLongitude()).isNull();
    }

    // ── GPS coordinate passthrough ────────────────────────────────────────────

    @Test
    void coordinates_echoedBackInResolvedFields() {
        when(wardLookupService.findWardByCoordinates(LAT, LON))
                .thenReturn(WardLookupResult.builder()
                        .wardId(WARD_ID)
                        .wardName("Dalanwala")
                        .build());
        when(wardRepository.findGoverningBodyIdById(WARD_ID))
                .thenReturn(Optional.of(GOVERNING_BODY_ID));

        LocationRoutingResult result = service.resolveRouting(
                LocationRoutingRequest.builder().latitude(LAT).longitude(LON).build());

        assertThat(result.getResolvedLatitude()).isEqualTo(LAT);
        assertThat(result.getResolvedLongitude()).isEqualTo(LON);
    }

    // ── formattedAddress is always null ───────────────────────────────────────

    @Test
    void formattedAddress_alwaysNull_wardLocatorDoesNotReturnIt() {
        when(wardLookupService.findWardByCoordinates(LAT, LON))
                .thenReturn(WardLookupResult.builder()
                        .wardId(WARD_ID)
                        .wardName("Dalanwala")
                        .build());
        when(wardRepository.findGoverningBodyIdById(WARD_ID))
                .thenReturn(Optional.of(GOVERNING_BODY_ID));

        LocationRoutingResult result = service.resolveRouting(
                LocationRoutingRequest.builder().latitude(LAT).longitude(LON).build());

        assertThat(result.getFormattedAddress())
                .as("formattedAddress is always null — ward-locator returns wardNo+wardName only; "
                        + "geocoder integration pending")
                .isNull();
    }

    // ── governingBodyId from local DB, not from ward-locator ─────────────────

    @Test
    void governingBodyId_derivedFromLocalDb_notFromMunicipalityId() {
        UUID differentIdFromWardLocator = UUID.fromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF");

        // ward-locator returns a municipalityId (legacy field)
        when(wardLookupService.findWardByCoordinates(LAT, LON))
                .thenReturn(WardLookupResult.builder()
                        .wardId(WARD_ID)
                        .wardName("Dalanwala")
                        .municipalityId(differentIdFromWardLocator)  // external, untrusted
                        .build());

        // local DB returns the authoritative governing body ID
        when(wardRepository.findGoverningBodyIdById(WARD_ID))
                .thenReturn(Optional.of(GOVERNING_BODY_ID));

        LocationRoutingResult result = service.resolveRouting(
                LocationRoutingRequest.builder().latitude(LAT).longitude(LON).build());

        assertThat(result.getGoverningBodyId())
                .as("governingBodyId must come from the local DB FK chain, not from ward-locator municipalityId")
                .isEqualTo(GOVERNING_BODY_ID);
    }

    // ── Ward-locator returns null ─────────────────────────────────────────────

    @Test
    void wardLookupReturnsNull_wardIdAndGoverningBodyIdAreNull() {
        when(wardLookupService.findWardByCoordinates(LAT, LON)).thenReturn(null);

        LocationRoutingResult result = service.resolveRouting(
                LocationRoutingRequest.builder().latitude(LAT).longitude(LON).build());

        assertThat(result.getWardId()).isNull();
        assertThat(result.getGoverningBodyId()).isNull();
        // Coordinates are still echoed back even when ward lookup fails
        assertThat(result.getResolvedLatitude()).isEqualTo(LAT);
        assertThat(result.getResolvedLongitude()).isEqualTo(LON);
    }

    // ── Ward found but not in local DB (partial result) ──────────────────────

    @Test
    void wardFoundByLocatorButNotInDb_wardIdNull_governingBodyIdNull() {
        // HttpWardLookupService returns a partial result when ward name is not in DB
        when(wardLookupService.findWardByCoordinates(LAT, LON))
                .thenReturn(WardLookupResult.builder()
                        .wardName("Unknown Ward")
                        // wardId is null — ward not seeded yet
                        .build());

        LocationRoutingResult result = service.resolveRouting(
                LocationRoutingRequest.builder().latitude(LAT).longitude(LON).build());

        assertThat(result.getWardId()).isNull();
        assertThat(result.getGoverningBodyId()).isNull();
        assertThat(result.getWardName()).isEqualTo("Unknown Ward");
    }
}
