package com.cirm.platform.ai.entity;

import com.cirm.platform.ai.domain.enums.ConversationStatus;
import com.cirm.platform.ai.domain.enums.ConversationType;
import com.cirm.platform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ai_conversation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversation extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false, length = 30)
    private ConversationType conversationType;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConversationStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

   @OneToMany(
        mappedBy = "conversation",
        fetch = FetchType.LAZY
        )

    private List<AiMessage> messages = new ArrayList<>();
}
