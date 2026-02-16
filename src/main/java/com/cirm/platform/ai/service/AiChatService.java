package com.cirm.platform.ai.service;

import com.cirm.platform.ai.domain.enums.ConversationStatus;
import com.cirm.platform.ai.domain.enums.ConversationType;
import com.cirm.platform.ai.domain.enums.MessageRole;
import com.cirm.platform.ai.entity.AiConversation;
import com.cirm.platform.ai.entity.AiMessage;
import com.cirm.platform.ai.repository.AiConversationRepository;
import com.cirm.platform.ai.repository.AiMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
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
                                            UUID municipalityId,
                                            ConversationType type) {

        AiConversation conversation = AiConversation.builder()
                .conversationType(type)
                .userId(userId)
                .municipalityId(municipalityId)
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
