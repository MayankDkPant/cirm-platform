package com.cxp.platform.ai.repository;

import com.cxp.platform.ai.domain.enums.ConversationStatus;
import com.cxp.platform.ai.domain.AiConversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsible for persistence operations on AI conversations.
 *
 * This repository is used by the AI chat service to:
 * - Create new conversation sessions
 * - Fetch active conversations for a user
 * - Support multi-tenant filtering by governing body
 *
 * No business logic should live here. Only data access.
 */
public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

    List<AiConversation> findByGoverningBodyId(UUID governingBodyId);

    Optional<AiConversation> findByIdAndGoverningBodyId(UUID id, UUID governingBodyId);

    List<AiConversation> findByUserIdAndStatus(UUID userId, ConversationStatus status);
}
