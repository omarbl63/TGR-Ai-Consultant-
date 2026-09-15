package com.pfe.adminagent.conversation;

import com.pfe.adminagent.common.exception.ApiException;
import com.pfe.adminagent.common.exception.ResourceNotFoundException;
import com.pfe.adminagent.conversation.domain.Conversation;
import com.pfe.adminagent.conversation.domain.Message;
import com.pfe.adminagent.conversation.domain.MessageRole;
import com.pfe.adminagent.conversation.dto.ConversationDetailDto;
import com.pfe.adminagent.conversation.dto.ConversationDto;
import com.pfe.adminagent.conversation.dto.MessageDto;
import com.pfe.adminagent.conversation.repository.ConversationRepository;
import com.pfe.adminagent.conversation.repository.MessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages chat threads and messages. The AI reply is produced later by the
 * orchestrator (Phase 4); this service owns persistence and ownership checks.
 */
@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public ConversationDto create(UUID userId, String title) {
        Conversation conversation = new Conversation(userId, title != null ? title : "Nouvelle conversation");
        return ConversationDto.from(conversationRepository.save(conversation));
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> listForUser(UUID userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ConversationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailDto getDetail(UUID conversationId, UUID userId) {
        Conversation c = requireOwned(conversationId, userId);
        List<MessageDto> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream().map(MessageDto::from).toList();
        return new ConversationDetailDto(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt(), messages);
    }

    @Transactional
    public MessageDto postUserMessage(UUID conversationId, UUID userId, String content) {
        Conversation c = requireOwned(conversationId, userId);
        Message saved = appendMessage(c, MessageRole.USER, content, null);
        return MessageDto.from(saved);
    }

    /** Appends an assistant (AI) message. Used by the orchestrator. */
    @Transactional
    public MessageDto addAssistantMessage(UUID conversationId, String content, Map<String, Object> metadata) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));
        return MessageDto.from(appendMessage(c, MessageRole.ASSISTANT, content, metadata));
    }

    /** Appends a message (any role) and bumps the conversation's updated_at. */
    @Transactional
    public Message appendMessage(Conversation conversation, MessageRole role, String content,
                                 Map<String, Object> metadata) {
        Message saved = messageRepository.save(new Message(conversation.getId(), role, content, metadata));
        conversation.touch();
        conversationRepository.save(conversation);
        return saved;
    }

    @Transactional(readOnly = true)
    public Conversation requireOwned(UUID conversationId, UUID userId) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));
        if (!c.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette conversation ne vous appartient pas");
        }
        return c;
    }
}
