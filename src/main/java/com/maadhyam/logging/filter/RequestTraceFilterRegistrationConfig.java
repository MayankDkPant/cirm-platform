package com.maadhyam.logging.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestTraceFilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<RequestTraceFilter> requestTraceFilterRegistration(RequestTraceFilter requestTraceFilter) {
        FilterRegistrationBean<RequestTraceFilter> registration = new FilterRegistrationBean<>(requestTraceFilter);
        registration.setEnabled(false);
        return registration;
    }
}
