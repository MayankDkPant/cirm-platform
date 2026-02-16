package com.cirm.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication

// Scan ONLY modules we have migrations for
@EntityScan(basePackages = {
        "com.cirm.platform.admin.domain",
        "com.cirm.platform.integration.domain"
})

// Scan ONLY repositories from these modules
@EnableJpaRepositories(basePackages = {
        "com.cirm.platform.admin.repository",
        "com.cirm.platform.integration.repository"
})
public class CirmPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CirmPlatformApplication.class, args);
    }
}
