package com.cxp.platform.location.application;

import com.cxp.platform.location.domain.WardLookupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/**
 * Retained for local/test use only — not registered as a Spring bean.
 * Replaced by HttpWardLookupService for all running environments.
 */
@Slf4j
public class MockWardLookupService implements WardLookupService {

    private final JdbcTemplate jdbcTemplate;
    private volatile WardLookupResult cachedResult;

    public MockWardLookupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public WardLookupResult findWardByCoordinates(Double latitude, Double longitude) {
        if (cachedResult == null) {
            synchronized (this) {
                if (cachedResult == null) {
                    cachedResult = resolveFromDb();
                    log.warn(
                            "mock_ward_lookup_resolved wardId={} — replace with real GIS service before production",
                            cachedResult.getWardId()
                    );
                }
            }
        }
        return cachedResult;
    }

    private WardLookupResult resolveFromDb() {
        return jdbcTemplate.query(
                "SELECT w.id AS ward_id, gb.id AS gb_id " +
                "FROM ward w " +
                "JOIN governing_body gb ON gb.id = w.governing_body_id " +
                "LIMIT 1",
                rs -> {
                    if (rs.next()) {
                        return WardLookupResult.builder()
                                .wardId(rs.getObject("ward_id", UUID.class))
                                .wardName("Ward (GIS pending)")
                                .municipalityId(rs.getObject("gb_id", UUID.class))
                                .municipalityName("Municipality (GIS pending)")
                                .build();
                    }
                    throw new IllegalStateException(
                            "No ward data found in DB. Ensure governance seed migrations have run.");
                }
        );
    }
}
