package com.roomconnect.modules.chat.service;

import com.roomconnect.modules.chat.entity.Conversation;
import com.roomconnect.modules.chat.entity.Message;
import com.roomconnect.modules.chat.repository.ConversationRepository;
import com.roomconnect.modules.chat.repository.MessageRepository;
import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.shared.exception.ForbiddenException;
import com.roomconnect.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ListingRepository listingRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Conversation getOrCreateConversation(UUID listingId, UUID visitorId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));

        return conversationRepository.findByListingIdAndVisitorId(listingId, visitorId)
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .listingId(listingId)
                            .visitorId(visitorId)
                            .ownerId(listing.getOwnerId())
                            .build();
                    return conversationRepository.save(newConv);
                });
    }

    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(UUID userId) {
        return conversationRepository.findByOwnerIdOrVisitorIdOrderByLastMessageAtDesc(userId, userId);
    }

    @Transactional(readOnly = true)
    public Page<Message> getConversationMessages(UUID conversationId, UUID requesterId, int page, int size) {
        Conversation conv = getConversationOrThrow(conversationId);
        assertParticipant(conv, requesterId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return messageRepository.findByConversationId(conversationId, pageable);
    }

    @Transactional
    public Message saveAndBroadcastMessage(UUID senderId, UUID conversationId, String body) {
        Conversation conv = getConversationOrThrow(conversationId);
        assertParticipant(conv, senderId);

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .body(body)
                .build();
        Message saved = messageRepository.save(message);

        conv.setLastMessageAt(OffsetDateTime.now());
        conversationRepository.save(conv);

        // Broadcast to WebSocket subscribers for this conversation
        String topic = "/topic/conversation/" + conversationId;
        log.info("Broadcasting message to WebSocket topic: {}", topic);
        messagingTemplate.convertAndSend(topic, saved);

        // Also notify recipient if they are not listening (in-app alert/badge push)
        UUID recipientId = conv.getOwnerId().equals(senderId) ? conv.getVisitorId() : conv.getOwnerId();
        messagingTemplate.convertAndSend("/topic/notifications/" + recipientId, saved);

        return saved;
    }

    private Conversation getConversationOrThrow(UUID id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + id));
    }

    private void assertParticipant(Conversation conv, UUID userId) {
        if (!conv.getOwnerId().equals(userId) && !conv.getVisitorId().equals(userId)) {
            throw new ForbiddenException("You are not a participant in this conversation");
        }
    }
}
