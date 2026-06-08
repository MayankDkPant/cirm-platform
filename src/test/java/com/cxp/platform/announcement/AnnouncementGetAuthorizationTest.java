package com.cxp.platform.announcement;

import com.cxp.platform.announcement.controller.AnnouncementController;
import com.cxp.platform.announcement.domain.AnnouncementPriority;
import com.cxp.platform.announcement.domain.AnnouncementStatus;
import com.cxp.platform.announcement.domain.AnnouncementTargetScope;
import com.cxp.platform.announcement.dto.AnnouncementResponse;
import com.cxp.platform.announcement.service.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization tests for GET /api/v1/announcements/{id}.
 *
 * Self-contained: minimal @EnableWebSecurity + @EnableMethodSecurity context so
 * @PreAuthorize("hasRole('OPERATOR')") is enforced. No JPA, no Flyway, no Spring Boot
 * auto-configuration. springSecurity() wires the FilterChainProxy into MockMvc.
 *
 * @WithMockUser(roles = "OPERATOR")     → platform JWT with ROLE_OPERATOR
 * @WithMockUser(roles = "authenticated") → Supabase citizen JWT with ROLE_authenticated
 *
 * Covered assertions:
 *   1. Citizen token → 403 Forbidden (pre-empted at method security layer)
 *   2. Operator token → 200 OK (service invoked, response returned)
 *   3. Service not invoked when authorization fails (citizen case)
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AnnouncementGetAuthorizationTest.TestConfig.class)
class AnnouncementGetAuthorizationTest {

    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID GOVERNING_ID    = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    @EnableWebMvc
    static class TestConfig {

        @Bean
        AnnouncementService announcementService() {
            return mock(AnnouncementService.class);
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

    @Autowired
    private AnnouncementService announcementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        reset(announcementService);
    }

    @Test
    @WithMockUser(roles = "authenticated")
    void citizenToken_getById_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/{id}", ANNOUNCEMENT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "authenticated")
    void citizenToken_getById_serviceNotInvoked() throws Exception {
        mockMvc.perform(get("/api/v1/announcements/{id}", ANNOUNCEMENT_ID))
                .andExpect(status().isForbidden());

        verify(announcementService, never()).get(any());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorToken_getById_returns200() throws Exception {
        AnnouncementResponse response = new AnnouncementResponse(
                ANNOUNCEMENT_ID,
                "Water outage",
                "Supply disrupted.",
                null, null,
                AnnouncementPriority.IMPORTANT,
                AnnouncementStatus.PUBLISHED,
                GOVERNING_ID,
                AnnouncementTargetScope.CITY,
                null, null, null, null, null,
                null, null,
                false, null, null, null
        );
        when(announcementService.get(ANNOUNCEMENT_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/announcements/{id}", ANNOUNCEMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ANNOUNCEMENT_ID.toString()))
                .andExpect(jsonPath("$.title").value("Water outage"));

        verify(announcementService).get(ANNOUNCEMENT_ID);
    }
}
