package com.cxp.platform.auth.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Citizen civic profile provisioning request for POST /api/v1/users/provision. " +
                      "Email is excluded — it is extracted from the validated JWT claims, making spoofed body values impossible. " +
                      "Ward resolution uses GPS coordinates (Path A, preferred) or an explicit wardId (Path B).")
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserProvisionRequest(

        @Schema(description = "Citizen's full legal name.", nullable = true)
        String fullName,

        @Schema(description = "Citizen's mobile number used for OTP authentication.", nullable = true)
        String mobileNumber,

        @Schema(description = "Free-text address line for the citizen's registered location.", nullable = true)
        String address,

        @Schema(description = "GPS latitude for Path A ward resolution (highest-fidelity path). " +
                              "When provided alongside longitude, the backend resolves the ward automatically. " +
                              "Takes precedence over an explicit wardId.",
                nullable = true)
        Double latitude,

        @Schema(description = "GPS longitude for Path A ward resolution. Same semantics as latitude.",
                nullable = true)
        Double longitude,

        @Schema(description = "Postal / PIN code (Indian convention). " +
                              "Also accepted as 'postalCode' for backward compatibility.",
                nullable = true)
        @JsonAlias("postalCode") String pinCode,

        @Schema(description = "Explicit ward identifier for Path B resolution — used when GPS is unavailable " +
                              "or the citizen manually corrects their ward via the governance discovery hierarchy. " +
                              "Must be a UUID from GET /governing-bodies/{id}/wards. " +
                              "Ignored when GPS coordinates are present and resolve successfully.",
                nullable = true)
        UUID wardId
) {}
