package com.pfe.adminagent.conversation.repository;

import com.pfe.adminagent.conversation.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
