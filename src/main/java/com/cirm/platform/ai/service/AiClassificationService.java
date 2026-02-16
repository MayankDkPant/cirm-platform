package com.cirm.platform.ai.service;

import com.cirm.platform.ai.client.AiClassificationResponse;
import com.cirm.platform.ai.client.AiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service responsible for using AI to classify and extract structured
 * information from natural language complaint text.
 *
 * This service acts as a bridge between the Complaint domain and the AI layer.
 *
 * WHY THIS EXISTS:
 * The Complaint module should NOT directly call external AI services.
 * This service isolates AI integration and keeps business logic clean.
 *
 * Current responsibilities:
 * - Send complaint text to Python AI service
 * - Return structured classification result
 * - Provide a single entry point for all AI-based complaint analysis
 *
 * Future responsibilities:
 * - Add prompt engineering
 * - Add fallback strategies
 * - Add confidence threshold logic
 * - Add ward/zone extraction
 */
@Service
@RequiredArgsConstructor
public class AiClassificationService {

    private final AiClient aiClient;

    /**
     * Sends complaint text to AI service and returns classification result.
     */
    public AiClassificationResponse classifyComplaint(String complaintText) {
        return aiClient.classify(complaintText);
    }
}
