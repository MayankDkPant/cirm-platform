package com.cxp.platform.location.application;

import com.cxp.platform.governance.domain.Ward;
import com.cxp.platform.governance.repository.WardRepository;
import com.cxp.platform.integration.wardlocator.WardLocatorClient;
import com.cxp.platform.integration.wardlocator.WardLocatorResponse;
import com.cxp.platform.location.domain.WardLookupResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resolves ward and governing body from coordinates using the external ward-locator service
 * and the local governance DB.
 *
 * The ward-locator service owns geographic polygon data (lat/lon → wardNo + wardName).
 * This service bridges that result back to the internal Ward entity to obtain the UUID
 * and governing body FK needed for workflow routing.
 *
 * Graceful degradation: if the ward-locator is unavailable or the ward name is not found
 * in the DB, null is returned. The caller (DefaultLocationIntelligenceService) treats a
 * null ward as an unresolvable jurisdiction and rejects the request with a 422.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpWardLookupService implements WardLookupService {

    private final WardLocatorClient wardLocatorClient;
    private final WardRepository wardRepository;

    @Override
    @Transactional(readOnly = true)
    public WardLookupResult findWardByCoordinates(Double latitude, Double longitude) {
        Optional<WardLocatorResponse> located = wardLocatorClient.locate(latitude, longitude);
        if (located.isEmpty()) {
            log.warn("ward_lookup_no_result lat={} lon={} — ward-locator returned empty", latitude, longitude);
            return null;
        }

        WardLocatorResponse geoWard = located.get();
        Optional<Ward> wardOpt = wardRepository.findByNameFetchingGoverningBody(geoWard.wardName());
        if (wardOpt.isEmpty()) {
            log.warn("ward_not_in_db wardName={} — partial result returned", geoWard.wardName());
            // Return partial: name is known but no UUID yet (ward data may be unseeded)
            return WardLookupResult.builder()
                    .wardName(geoWard.wardName())
                    .build();
        }

        Ward ward = wardOpt.get();
        log.debug("ward_resolved wardId={} wardName={} gbId={}", ward.getId(), ward.getName(), ward.getGoverningBody().getId());
        return WardLookupResult.builder()
                .wardId(ward.getId())
                .wardName(ward.getName())
                .municipalityId(ward.getGoverningBody().getId())
                .build();
    }
}
