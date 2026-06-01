package com.cxp.platform.location.application;

import com.cxp.platform.governance.repository.WardRepository;
import com.cxp.platform.location.domain.LocationRoutingRequest;
import com.cxp.platform.location.domain.LocationRoutingResult;
import com.cxp.platform.location.domain.WardLookupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves ward routing metadata from GPS coordinates supplied by the caller.
 *
 * Architecture decision: the backend does NOT perform geocoding (address → coordinates).
 * The mobile app is responsible for supplying GPS coordinates from the device.
 * If no coordinates are provided, routing is unresolved and an empty result is returned.
 *
 * Flow when coordinates present (Case A):
 *   lat/lon → ward-locator (external HTTP service) → WardLookupResult
 *            → WardRepository → governingBodyId
 *            → LocationRoutingResult
 *
 * Flow when coordinates absent (Case B):
 *   Returns empty LocationRoutingResult (all fields null).
 *   Callers decide how to handle the missing geography
 *   (e.g. CitizenProvisioningService preserves existing profile geography;
 *    ServiceRequestService falls back to the user's profile ward).
 *
 * governingBodyId is derived from the Ward → GoverningBody FK, making it
 * authoritative and independent of JWT/session context.
 */
@Slf4j
@Service
public class DefaultLocationIntelligenceService implements LocationIntelligenceService {

    private final Optional<WardLookupService> wardLookupService;
    private final WardRepository wardRepository;

    public DefaultLocationIntelligenceService(
            Optional<WardLookupService> wardLookupService,
            WardRepository wardRepository) {
        this.wardLookupService = wardLookupService;
        this.wardRepository = wardRepository;
    }

    @Override
    public LocationRoutingResult resolveRouting(LocationRoutingRequest request) {
        Double lat = request.getLatitude();
        Double lon = request.getLongitude();

        if (!hasCoordinates(lat, lon)) {
            log.debug("location_no_coords — no GPS coordinates provided; ward lookup skipped");
            return LocationRoutingResult.builder().build();
        }

        WardLookupResult ward = wardLookupService
                .map(svc -> svc.findWardByCoordinates(lat, lon))
                .orElse(null);

        UUID governingBodyId = resolveGoverningBodyId(ward);

        log.debug("location_resolved lat={} lon={} wardId={} governingBodyId={}",
                lat, lon,
                ward != null ? ward.getWardId() : null,
                governingBodyId);

        return LocationRoutingResult.builder()
                .governingBodyId(governingBodyId)
                .municipalityId(ward != null ? ward.getMunicipalityId() : null)
                .municipalityName(ward != null ? ward.getMunicipalityName() : null)
                .wardId(ward != null ? ward.getWardId() : null)
                .wardName(ward != null ? ward.getWardName() : null)
                .resolvedLatitude(lat)
                .resolvedLongitude(lon)
                .build();
    }

    private UUID resolveGoverningBodyId(WardLookupResult ward) {
        if (ward == null || ward.getWardId() == null) {
            return null;
        }
        return wardRepository.findGoverningBodyIdById(ward.getWardId()).orElse(null);
    }

    private boolean hasCoordinates(Double lat, Double lon) {
        return lat != null && lon != null;
    }
}
