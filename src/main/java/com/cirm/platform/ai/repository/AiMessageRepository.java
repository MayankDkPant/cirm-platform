package com.cirm.platform.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cirm.platform.ai.entity.AiMessage;

import java.util.List;
import java.util.UUID;

/**
 * Repository responsible for storing and retrieving AI chat messages.
 *
 * Primary use cases:
 * - Save user prompts and AI responses
 * - Fetch recent messages for context building
 * - Support analytics and usage tracking
 */
public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    /**
     * Fetch last N messages ordered newest first.
     * Used to build context window for LLM.
     */
    List<AiMessage> findTop20ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

}
