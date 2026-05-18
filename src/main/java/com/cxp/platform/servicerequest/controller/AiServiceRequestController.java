package com.cxp.platform.servicerequest.controller;

import com.cxp.platform.servicerequest.dto.AiServiceRequestAnalyzeRequest;
import com.cxp.platform.servicerequest.dto.AiServiceRequestAnalyzeResponse;
import com.cxp.platform.servicerequest.service.AiServiceRequestAnalyzeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/service-requests")
@RequiredArgsConstructor
public class AiServiceRequestController {

    private final AiServiceRequestAnalyzeService analyzeService;

    @PostMapping("/analyze")
    public AiServiceRequestAnalyzeResponse analyze(@Valid @RequestBody AiServiceRequestAnalyzeRequest request) {
        return analyzeService.analyze(request.text());
    }
}
