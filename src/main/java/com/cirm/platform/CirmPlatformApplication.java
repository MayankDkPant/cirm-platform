package com.cirm.platform;

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
@EntityScan(basePackages = "com.cirm.platform")
@EnableJpaRepositories(basePackages = "com.cirm.platform")
@EnableScheduling
public class CirmPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CirmPlatformApplication.class, args);
    }
}
