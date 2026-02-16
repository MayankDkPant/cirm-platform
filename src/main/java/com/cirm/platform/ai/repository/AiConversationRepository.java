package com.cirm.platform.ai.repository;

import com.cirm.platform.ai.domain.enums.ConversationStatus;
import com.cirm.platform.ai.entity.AiConversation;

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
 * - Support multi-tenant filtering by municipality
 *
 * No business logic should live here. Only data access.
 */
public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

    Optional<AiConversation> findByIdAndMunicipalityId(UUID id, UUID municipalityId);

    List<AiConversation> findByUserIdAndStatus(UUID userId, ConversationStatus status);
}
