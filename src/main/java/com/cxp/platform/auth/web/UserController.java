package com.cxp.platform.auth.web;

import com.cxp.platform.auth.application.CitizenProvisioningService;
import com.cxp.platform.auth.application.CurrentUserProvider;
import com.cxp.platform.auth.web.dto.CitizenProfileResponse;
import com.cxp.platform.auth.web.dto.UserProvisionRequest;
import com.cxp.platform.auth.web.dto.UserProvisionResponse;
import com.cxp.platform.api.error.ApiError;
import com.cxp.platform.common.openapi.StandardApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final CitizenProvisioningService citizenProvisioningService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(
            summary     = "Provision a citizen profile",
            description = "Creates or updates the civic profile linked to the authenticated Supabase JWT. " +
                          "Identity (email) is extracted from the verified JWT claim — client-supplied " +
                          "identity fields are ignored. Safe to call on every app launch (upsert semantics). " +
                          "Requires: authenticated citizen session."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Profile provisioned or updated")
    @ApiResponse(responseCode = "400",
            description = "Validation failure or unresolvable wardId supplied in the request body.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/provision")
    public ResponseEntity<UserProvisionResponse> provision(@Valid @RequestBody UserProvisionRequest request) {
        String verifiedEmail = currentUserProvider.getCurrentUserPhone();
        log.info("USER_PROVISION_REQUEST verifiedEmail={}", verifiedEmail);
        return ResponseEntity.ok(citizenProvisioningService.provision(verifiedEmail, request));
    }

    @Operation(
            summary     = "Retrieve the authenticated citizen's profile",
            description = "Returns the civic profile for the caller's Supabase identity, " +
                          "including stored geography (ward, city, governing body) and display name. " +
                          "Requires: authenticated citizen session."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "Citizen profile returned")
    @ApiResponse(responseCode = "400",
            description = "Profile has not been provisioned yet. Call POST /api/v1/users/provision first.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/me")
    public ResponseEntity<CitizenProfileResponse> me() {
        return ResponseEntity.ok(citizenProvisioningService.getMyProfile());
    }
}
