package com.cxp.platform.announcement;

import com.cxp.platform.announcement.controller.AnnouncementController;
import com.cxp.platform.announcement.service.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security routing tests for the public announcement feed.
 *
 * Fully self-contained: uses a minimal @Configuration that owns both the security
 * rules and the mocked controller. No Spring Boot auto-configuration, no JPA, no Flyway.
 * springSecurity() applies the FilterChainProxy from the test context to MockMvc.
 *
 * SecurityFilterChain mirrors the exact production routing rule in SecurityConfig:
 *   .requestMatchers(HttpMethod.GET, "/api/v1/announcements/feed").permitAll()
 *   .requestMatchers("/api/**").authenticated()
 *
 * Tests prove that the exact-path permit covers only /feed.
 * Any sub-path (/feed/admin, etc.) falls through to authenticated() and returns
 * 4xx without a token — the wildcard gap that PR3 closes.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = FeedEndpointSecurityTest.TestConfig.class)
class FeedEndpointSecurityTest {

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    static class TestConfig {

        @Bean
        AnnouncementService announcementService() {
            AnnouncementService service = mock(AnnouncementService.class);
            when(service.getPublicFeed(any())).thenReturn(List.of());
            return service;
        }

        @Bean
        AnnouncementController announcementController(AnnouncementService announcementService) {
            return new AnnouncementController(announcementService);
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.GET, "/api/v1/announcements/feed").permitAll()
                            .requestMatchers("/api/**").authenticated()
                            .anyRequest().denyAll()
                    )
                    .build();
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void publicFeed_withoutToken_isPermitted() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/feed"))
                .andExpect(status().isOk());
    }

    @Test
    void feedSubPath_withoutToken_isProtected() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/feed/admin"))
                .andExpect(status().is4xxClientError());
    }
}
