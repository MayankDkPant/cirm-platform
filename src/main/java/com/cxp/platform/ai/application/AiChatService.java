package com.cxp.platform.ai.application;

import com.cxp.platform.ai.domain.enums.ConversationStatus;
import com.cxp.platform.ai.domain.enums.ConversationType;
import com.cxp.platform.ai.domain.enums.MessageRole;
import com.cxp.platform.ai.domain.AiConversation;
import com.cxp.platform.ai.domain.AiMessage;
import com.cxp.platform.ai.repository.AiConversationRepository;
import com.cxp.platform.ai.repository.AiMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application service orchestrating use cases for the module.
 *
 * Core service responsible for orchestrating AI chat interactions.
 *
 * Responsibilities:
 * - Create new conversation sessions
 * - Store user messages
 * - Fetch recent conversation history (context window)
 * - Persist AI responses
 *
 * IMPORTANT:
 * This service DOES NOT call the AI provider yet.
 * That integration will be added after the chat pipeline is complete.
 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;

    /**
     * Creates a new AI conversation session for a user.
     */
    public AiConversation startConversation(UUID userId,
                                            UUID governingBodyId,
                                            ConversationType type) {

        AiConversation conversation = AiConversation.builder()
                .conversationType(type)
                .userId(userId)
                .governingBodyId(governingBodyId)
                .status(ConversationStatus.ACTIVE)
                .startedAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build();

        return conversationRepository.save(conversation);
    }

    /**
     * Saves a user message into the conversation.
     */
    public AiMessage saveUserMessage(AiConversation conversation, String message) {

        AiMessage aiMessage = AiMessage.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(message)
                .build();

        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        return messageRepository.save(aiMessage);
    }

    /**
     * Saves an AI generated response.
     * Metadata fields are optional and will be populated once AI integration is added.
     */
    public AiMessage saveAiResponse(AiConversation conversation,
                                    String response,
                                    String model,
                                    Integer promptTokens,
                                    Integer completionTokens,
                                    Integer totalTokens,
                                    Integer responseTimeMs) {

        AiMessage aiMessage = AiMessage.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(response)
                .model(model)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .responseTimeMs(responseTimeMs)
                .build();

        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        return messageRepository.save(aiMessage);
    }

    /**
     * Fetch last 20 messages to build LLM context window.
     */
    public List<AiMessage> getRecentMessages(UUID conversationId) {
        return messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId);
    }
}
