package com.cxp.platform.servicerequest.controller;

import com.cxp.platform.api.error.ApiError;
import com.cxp.platform.common.openapi.StandardApiErrors;
import com.cxp.platform.servicerequest.dto.AiServiceRequestAnalyzeRequest;
import com.cxp.platform.servicerequest.dto.AiServiceRequestAnalyzeResponse;
import com.cxp.platform.servicerequest.service.AiServiceRequestAnalyzeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Deprecated — duplicate of POST /api/v1/service-requests/analyze. Pending removal in Wave 7.
@Tag(name = "AI")
@RestController
@RequestMapping("/api/v1/ai/service-requests")
@RequiredArgsConstructor
public class AiServiceRequestController {

    private final AiServiceRequestAnalyzeService analyzeService;

    @Deprecated
    @Operation(
            deprecated  = true,
            summary     = "AI service-request enrichment preview (deprecated — use /service-requests/analyze)",
            description = "DEPRECATED. Canonical replacement: POST /api/v1/service-requests/analyze — " +
                          "identical request/response contract, stable path. " +
                          "This endpoint is a legacy alias retained for one release cycle during client migration. " +
                          "It will be removed in Wave 7. " +
                          "SDK consumers must migrate to the canonical path before that release."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "AI enrichment preview returned — same shape as /service-requests/analyze")
    @ApiResponse(responseCode = "400",
            description = "Validation failure — required fields missing or invalid.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/analyze")
    public AiServiceRequestAnalyzeResponse analyze(@Valid @RequestBody AiServiceRequestAnalyzeRequest request) {
        return analyzeService.analyze(request.title(), request.description(), request.latitude(), request.longitude());
    }
}
