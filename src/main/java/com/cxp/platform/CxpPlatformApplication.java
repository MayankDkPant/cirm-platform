package com.cxp.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.cxp.platform", "com.maadhyam.logging"})
@EntityScan(basePackages = {"com.cxp.platform", "com.maadhyam.logging"})
@EnableJpaRepositories(basePackages = {"com.cxp.platform", "com.maadhyam.logging"})
@EnableScheduling
public class CxpPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CxpPlatformApplication.class, args);
    }
}
