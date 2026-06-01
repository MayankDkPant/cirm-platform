package com.cxp.platform.ai.api;

import com.cxp.platform.ai.application.AiChatService;
import com.cxp.platform.ai.domain.AiConversation;
import com.cxp.platform.ai.domain.enums.ConversationType;
import com.cxp.platform.auth.application.CurrentUserProvider;
import com.cxp.platform.common.tenant.TenantContext;
import com.cxp.platform.api.error.ApiError;
import com.cxp.platform.common.openapi.StandardApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Authenticated AI chat endpoint.
 *
 * Identity contract (PR4):
 *   - Requires a valid Bearer JWT (falls under /api/** authenticated rule in SecurityConfig).
 *   - userId is derived from the authenticated SecurityContext — never from the request body.
 *   - governingBodyId is derived from TenantContext (operator sessions); null for citizen
 *     sessions whose Supabase JWT carries no tenant claim.
 *
 * TODO (future): move to Python AI sidecar, add streaming, operator-specific AI capabilities.
 */
@Tag(name = "AI")
@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Client-controlled fields: message and conversation type only.
     * Identity (userId, tenantId) is always server-derived.
     *
     * IDEMPOTENCY NOTE: every POST currently creates a new AiConversation. Duplicate
     * records on mobile retry are intentionally tolerated — this endpoint is a
     * placeholder; real AI pipeline will use a client-supplied conversationId for
     * session continuity, at which point conversation creation will become idempotent.
     */
    @io.swagger.v3.oas.annotations.media.Schema(
            name        = "AiChatRequest",
            description = "Input for the AI chat endpoint. " +
                          "Identity (userId, tenantId) is always server-derived from the JWT — " +
                          "never supply identity fields in the request body.")
    record ChatRequest(

            @io.swagger.v3.oas.annotations.media.Schema(description = "Citizen or operator message to the AI assistant.")
            String message,

            @io.swagger.v3.oas.annotations.media.Schema(description = "Conversation type classifying the session context.")
            ConversationType conversationType
    ) {}

    @io.swagger.v3.oas.annotations.media.Schema(
            name        = "AiChatResponse",
            description = "AI assistant reply. " +
                          "AI output is advisory enrichment only — it cannot alter lifecycle state or trigger escalations. " +
                          "Note: each call currently creates a new conversation session; " +
                          "client-supplied conversationId for session continuity is planned for a future release.")
    record ChatResponse(

            @io.swagger.v3.oas.annotations.media.Schema(description = "Newly created conversation session identifier.")
            UUID conversationId,

            @io.swagger.v3.oas.annotations.media.Schema(description = "AI-generated reply. Advisory — non-authoritative.")
            String reply
    ) {}

    @Operation(
            summary     = "Start an AI chat session",
            description = "Sends a message to the civic AI assistant and returns a reply. " +
                          "The caller's identity (userId, tenantId) is always derived server-side from the JWT — " +
                          "never supplied by the client. " +
                          "AI responses are advisory enrichment only: the AI cannot alter lifecycle state " +
                          "or trigger escalations. " +
                          "Note: each call currently creates a new conversation — session continuity " +
                          "via a client-supplied conversationId is planned for a future release."
    )
    @StandardApiErrors
    @ApiResponse(responseCode = "200", description = "AI reply returned")
    @ApiResponse(responseCode = "400",
            description = "Malformed request body or invalid conversationType.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        UUID governingBodyId = TenantContext.getOrNull();

        AiConversation conversation = aiChatService.startConversation(
                userId,
                governingBodyId,
                request.conversationType()
        );

        aiChatService.saveUserMessage(conversation, request.message());

        String mockReply = "AI response placeholder. Python service will generate real reply.";

        aiChatService.saveAiResponse(
                conversation,
                mockReply,
                "mock-model",
                0, 0, 0, 0
        );

        return new ChatResponse(conversation.getId(), mockReply);
    }
}
