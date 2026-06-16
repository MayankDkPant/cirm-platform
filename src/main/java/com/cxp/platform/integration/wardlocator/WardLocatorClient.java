package com.cxp.platform.integration.wardlocator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * HTTP adapter for the external ward-locator microservice.
 *
 * Translates GPS coordinates into ward identification. Failures are isolated here —
 * callers receive an empty Optional rather than an exception, so the intake workflow
 * continues even when this service is degraded.
 */
@Slf4j
@Component
public class WardLocatorClient {

    private final RestTemplate restTemplate;

    @Value("${ward-locator.base-url:http://localhost:8080}")
    private String baseUrl;

    public WardLocatorClient(@Qualifier("wardLocatorRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<WardLocatorResponse> locate(double lat, double lon) {
        String url = baseUrl + "/ward/locate";
        try {
            WardLocatorResponse response = restTemplate.postForObject(
                    url,
                    new WardLocatorRequest(lat, lon),
                    WardLocatorResponse.class
            );
            if (response == null || response.wardName() == null) {
                log.warn("ward_locator_empty_response lat={} lon={}", lat, lon);
                return Optional.empty();
            }
            log.debug("ward_located lat={} lon={} wardNo={} wardName={}", lat, lon, response.wardNo(), response.wardName());
            return Optional.of(response);
        } catch (Exception e) {
            log.error("ward_locator_unavailable lat={} lon={} error={}", lat, lon, e.getMessage());
            return Optional.empty();
        }
    }
}
