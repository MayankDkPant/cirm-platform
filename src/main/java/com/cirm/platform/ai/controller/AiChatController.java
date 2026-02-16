package com.cirm.platform.ai.controller;

import com.cirm.platform.ai.domain.enums.ConversationType;
import com.cirm.platform.ai.entity.AiConversation;
import com.cirm.platform.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller exposing endpoints for Conversational AI.
 *
 * This is the entry point for:
 * - Citizen chatbot
 * - Employee AI copilot
 *
 * For now, this endpoint:
 * 1) Creates a conversation if none exists
 * 2) Stores the user message
 * 3) Returns a MOCK AI response
 *
 * In the next phase this controller will call the Python AI service.
 */
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * Temporary request DTO for chat messages.
     */
    record ChatRequest(
            UUID userId,
            UUID municipalityId,
            String message,
            ConversationType conversationType
    ) {}

    /**
     * Temporary response DTO.
     */
    record ChatResponse(
            UUID conversationId,
            String reply
    ) {}

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {

        // 1️⃣ Start a new conversation (V1 always starts new session)
        AiConversation conversation = aiChatService.startConversation(
                request.userId(),
                request.municipalityId(),
                request.conversationType()
        );

        // 2️⃣ Save user message
        aiChatService.saveUserMessage(conversation, request.message());

        // 3️⃣ MOCK AI response (real integration next)
        String mockReply = "AI response placeholder. Python service will generate real reply.";

        aiChatService.saveAiResponse(
                conversation,
                mockReply,
                "mock-model",
                0,
                0,
                0,
                0
        );

        return new ChatResponse(conversation.getId(), mockReply);
    }
}
