package com.cxp.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application.
 *
 * Now scans ALL platform modules (admin, auth, complaint, ai, integration).
 */
@SpringBootApplication
@EntityScan(basePackages = "com.cxp.platform")
@EnableJpaRepositories(basePackages = "com.cxp.platform")
@EnableScheduling
public class CxpPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CxpPlatformApplication.class, args);
    }
}
