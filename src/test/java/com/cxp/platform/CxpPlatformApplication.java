package com.cxp.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication

// Scan ONLY modules we have migrations for
@EntityScan(basePackages = {
        "com.cxp.platform.admin.domain",
        "com.cxp.platform.integration.domain"
})

// Scan ONLY repositories from these modules
@EnableJpaRepositories(basePackages = {
        "com.cxp.platform.admin.repository",
        "com.cxp.platform.integration.repository"
})
public class CxpPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CxpPlatformApplication.class, args);
    }
}
