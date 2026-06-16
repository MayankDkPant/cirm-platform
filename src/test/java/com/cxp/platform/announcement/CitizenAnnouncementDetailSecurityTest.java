package com.cxp.platform.announcement;

import com.cxp.platform.announcement.controller.AnnouncementController;
import com.cxp.platform.announcement.domain.AnnouncementPriority;
import com.cxp.platform.announcement.domain.AnnouncementTargetScope;
import com.cxp.platform.announcement.dto.CitizenAnnouncementDetailResponse;
import com.cxp.platform.announcement.service.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security routing tests for GET /api/v1/announcements/{id}/public.
 *
 * Verifies the authentication requirement:
 *   - Anonymous (no Bearer JWT) → 401 Unauthorized
 *   - Authenticated citizen (Supabase JWT, ROLE_authenticated) → 200 OK
 *   - Authenticated operator (platform JWT, ROLE_OPERATOR) → 200 OK
 *
 * The TestConfig SecurityFilterChain mirrors the production rule in SecurityConfig:
 *   - GET /feed → permitAll (unchanged from PR3)
 *   - /api/**  → authenticated()
 *   - anything else → denyAll
 *
 * GET /{id}/public falls under /api/** → authenticated(). No special permitAll rule is
 * needed or present — that is exactly what these tests verify.
 *
 * Self-contained: no Spring Boot auto-configuration, no JPA, no Flyway.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = CitizenAnnouncementDetailSecurityTest.TestConfig.class)
class CitizenAnnouncementDetailSecurityTest {

    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    static class TestConfig {

        @Bean
        AnnouncementService announcementService() {
            AnnouncementService service = mock(AnnouncementService.class);
            when(service.getPublicDetail(any(UUID.class))).thenReturn(
                    new CitizenAnnouncementDetailResponse(
                            ANNOUNCEMENT_ID,
                            "Water supply disruption",
                            "Short summary.",
                            "Full body.",
                            "WATER",
                            AnnouncementPriority.IMPORTANT,
                            AnnouncementTargetScope.CITY,
                            null,  // publishedAt — null avoids JavaTimeModule requirement in test context
                            null,  // expiresAt
                            false,
                            null,  // tags
                            null,  // createdAt
                            List.of()
                    )
            );
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
                    // Return 401 for unauthenticated requests — matches production RestAuthenticationEntryPoint.
                    // Without this, Spring Security's default returns 403 for anonymous access.
                    .exceptionHandling(ex -> ex.authenticationEntryPoint(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth
                            // Mirror of SecurityConfig section ①. Keep in sync.
                            .requestMatchers(HttpMethod.GET, "/api/v1/announcements/feed").permitAll()
                            // Mirror of SecurityConfig section ②.
                            // GET /{id}/public is under /api/** — authentication required, no special rule.
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
    void anonymousRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/{id}/public", ANNOUNCEMENT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "authenticated")
    void citizenToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/{id}/public", ANNOUNCEMENT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/{id}/public", ANNOUNCEMENT_ID))
                .andExpect(status().isOk());
    }
}
