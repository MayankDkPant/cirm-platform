package com.cxp.platform.common.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Canonical OpenAPI governance configuration.
 *
 * SECURITY SCHEME: "bearerAuth" — Bearer JWT in the Authorization header.
 *   Accepts two JWT formats issued at runtime:
 *     • Platform JWT  — HS256, carries governingBodyId claim (operator sessions)
 *     • Supabase JWT  — ES256/RS256, issued by Supabase OAuth (citizen sessions)
 *   Both are validated by JwtAuthenticationFilter before Spring Security evaluates
 *   the route authorization rule.
 *
 * GLOBAL SECURITY REQUIREMENT: all documented operations require "bearerAuth" by default.
 *   Endpoints with no authentication (public routes) override this at the method level
 *   with @SecurityRequirements({}) — an explicit empty override removes the lock icon
 *   from Swagger UI, signalling to SDK consumers that the endpoint is unauthenticated.
 *
 * PUBLIC SURFACE INTENT: Swagger reflects the production API surface only.
 *   Local/dev controllers (@Profile("local")) are invisible here because they are not
 *   loaded in non-local Spring contexts — no additional filtering is required.
 *   Actuator endpoints are excluded via springdoc.show-actuator=false in application.yml.
 *
 * SDK-GENERATION ASSUMPTIONS:
 *   • Security scheme name "bearerAuth" is stable — do not rename without coordinating
 *     any generated SDK that depends on it.
 *   • The global security requirement means generated clients authenticate all calls
 *     by default; public-endpoint overrides generate un-authenticated client methods.
 */
@OpenAPIDefinition(
        info = @Info(
                title       = "CXP Platform API",
                version     = "v1",
                description = "Civic Exchange Platform — citizen service-request intake, " +
                              "AI enrichment, governance discovery, and announcement feed. " +
                              "Authenticated endpoints require a Bearer JWT " +
                              "(Platform HS256 for operators, Supabase ES256 for citizens). " +
                              "Public endpoints are explicitly marked.\n\n" +
                              "## Compatibility & Deprecation Policy\n\n" +
                              "The platform maintains backward-compatible aliases for one release cycle " +
                              "while clients migrate to canonical field names. " +
                              "All deprecated fields and endpoints are marked with a strikethrough in Swagger UI " +
                              "and carry `deprecated: true` in the generated OpenAPI schema — " +
                              "SDK generators will surface these as deprecated in generated client code.\n\n" +
                              "**Active transitional aliases (marked deprecated in schema):**\n" +
                              "- `ServiceRequestEvent.oldValue` → migrate to `oldStatus`\n" +
                              "- `ServiceRequestEvent.newValue` → migrate to `newStatus`\n" +
                              "- `ServiceRequestEvent.changedByRole` → migrate to `actorType`\n" +
                              "- `CitizenProfile.appUserId` → migrate to `id`\n" +
                              "- `CitizenProfile.postalCode` → migrate to `pinCode`\n" +
                              "- `UserProvision.appUserId` → migrate to `id`\n" +
                              "- `ServiceRequestCreate.aiConversationId` → not replaced; " +
                                "field is legacy from a discontinued conversational flow\n" +
                              "- Pagination `number` field → migrate to `page`\n\n" +
                              "**Deprecated endpoints:**\n" +
                              "- `POST /api/v1/ai/service-requests/analyze` → migrate to " +
                                "`POST /api/v1/service-requests/analyze` (identical contract)"
        ),
        security = @SecurityRequirement(name = "bearerAuth"),
        // Canonical tag registry — all domain groupings are declared here.
        // Controllers reference these names with @Tag(name = "...").
        // Do not introduce tags in controllers without adding them here first.
        tags = {
                @Tag(
                        name        = "Users",
                        description = "Citizen profile lifecycle — provision and retrieve the civic " +
                                      "identity tied to a Supabase JWT. All operations require " +
                                      "an authenticated citizen session. Profile data drives " +
                                      "geography targeting, service-request routing, and " +
                                      "personalised announcement feeds."
                ),
                @Tag(
                        name        = "Service Requests",
                        description = "Civic complaint intake and lifecycle management. Citizens " +
                                      "submit, list, and view their own requests. Operators " +
                                      "manage status transitions (requires OPERATOR role). " +
                                      "AI enrichment preview (/analyze) is available pre-submit " +
                                      "and does not persist any data."
                ),
                @Tag(
                        name        = "Announcements",
                        description = "Civic announcement publication and consumption. The anonymous " +
                                      "public feed (/feed) requires no authentication. The personalised " +
                                      "citizen feed (/my-feed) resolves geography from the authenticated " +
                                      "citizen profile. Operator authoring (create, list) requires the " +
                                      "OPERATOR role."
                ),
                @Tag(
                        name        = "Governance Discovery",
                        description = "Read-only hierarchy traversal for onboarding and reference lookups: " +
                                      "states → districts → cities → governing bodies → wards / departments. " +
                                      "All responses return only {id, name} — no audit metadata. " +
                                      "Intended consumers: onboarding flow, dropdown population, " +
                                      "service-request submission, and external SDK integrations."
                ),
                @Tag(
                        name        = "AI",
                        description = "Civic AI capabilities. Currently: conversational AI chat " +
                                      "(/ai/chat) scoped to the authenticated citizen or operator session. " +
                                      "AI output is enrichment-only — it cannot change lifecycle state or " +
                                      "trigger escalations. All AI responses include a confidence signal."
                )
        }
)
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        in          = SecuritySchemeIn.HEADER,
        description = "Bearer JWT. Accepted formats: " +
                      "Platform HS256 (operator — carries governingBodyId claim) or " +
                      "Supabase ES256 (citizen — no tenantId claim). " +
                      "Header: Authorization: Bearer <token>"
)
@Configuration
public class OpenApiConfig {
    // Intentionally empty — all configuration is expressed via the class-level annotations above.
    // Do not scatter @Operation or @SecurityRequirement overrides across controllers;
    // use this class as the single source of truth for scheme-level governance.
}
