package com.cxp.platform.auth.web.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prevents Spring Boot from auto-registering JwtAuthenticationFilter as a standalone
 * servlet filter. The filter is added explicitly to the Spring Security filter chain
 * in SecurityConfig and LocalDevSecurityConfig; standalone registration would cause
 * it to execute a second time after the security chain completes (after the response
 * is already committed), which is unnecessary and can produce spurious log output.
 */
@Configuration
public class JwtFilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
