package com.cxp.platform.common.dev;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Creates dev-only beans — active ONLY on the "local" Spring profile.
 *
 * DevIdentityFilter must NOT be registered as a standard servlet filter.
 * It is added explicitly to the Spring Security filter chain in LocalDevSecurityConfig
 * so that it runs inside the security chain and does not double-fire as a servlet filter.
 *
 * Delete this class and DevIdentityFilter once the OTP verify endpoint and
 * JWT issuance are implemented for production auth.
 */
@Slf4j
@Configuration
@Profile("local")
public class LocalDevConfiguration {

    @PostConstruct
    public void logProfileActive() {
        log.info("local_dev_configuration_loaded profile=local");
    }

    @Bean
    public DevIdentityFilter devIdentityFilter(DevUserSeeder devUserSeeder) {
        return new DevIdentityFilter(devUserSeeder);
    }

    @Bean
    public FilterRegistrationBean<DevIdentityFilter> devFilterRegistration(DevIdentityFilter filter) {
        FilterRegistrationBean<DevIdentityFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
