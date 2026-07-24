package com.roomconnect.modules.chat.controller;

import com.roomconnect.modules.chat.entity.Conversation;
import com.roomconnect.modules.chat.entity.Message;
import com.roomconnect.modules.chat.service.ChatService;
import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.repository.ListingRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ListingRepository listingRepository;

    /** GET /api/chat/conversations — Get authenticated user's conversations */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(@AuthenticationPrincipal UUID currentUserId) {
        List<Conversation> convs = chatService.getUserConversations(currentUserId);
        List<ConversationResponse> response = convs.stream().map(this::mapToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /** GET /api/chat/conversations/{id}/messages — Get conversation messages paginated */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<Page<Message>> getMessages(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Message> messages = chatService.getConversationMessages(id, currentUserId, page, size);
        return ResponseEntity.ok(messages);
    }

    /** POST /api/chat/conversations/{id}/messages — Send a message via HTTP REST */
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<Message> sendMessage(
            @PathVariable UUID id,
            @RequestBody SendMessageHttpRequest request,
            @AuthenticationPrincipal UUID currentUserId) {
        Message message = chatService.saveAndBroadcastMessage(currentUserId, id, request.getBody());
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /** POST /api/chat/conversations — Create or get conversation for a listing */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> startConversation(
            @RequestParam UUID listingId,
            @AuthenticationPrincipal UUID currentUserId) {
        Conversation conv = chatService.getOrCreateConversation(listingId, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(conv));
    }

    private ConversationResponse mapToResponse(Conversation c) {
        Listing listing = c.getListingId() != null ? listingRepository.findById(c.getListingId()).orElse(null) : null;
        ConversationResponse r = new ConversationResponse();
        r.setId(c.getId());
        r.setListingId(c.getListingId());
        r.setOwnerId(c.getOwnerId());
        r.setVisitorId(c.getVisitorId());
        r.setCreatedAt(c.getCreatedAt());
        r.setLastMessageAt(c.getLastMessageAt());
        r.setListingTitle(c.getListingTitle() != null ? c.getListingTitle() : (listing != null ? listing.getTitle() : "Room Inquiry"));
        r.setListingRent(c.getListingRent() != null ? c.getListingRent() : (listing != null ? listing.getRentAmount() : null));
        r.setListingAddress(c.getListingAddress() != null ? c.getListingAddress() : (listing != null ? listing.getAddressText() : null));
        return r;
    }

    @Getter @Setter
    public static class SendMessageHttpRequest {
        private String body;
    }

    @Getter @Setter
    public static class ConversationResponse {
        private UUID id;
        private UUID listingId;
        private UUID ownerId;
        private UUID visitorId;
        private OffsetDateTime createdAt;
        private OffsetDateTime lastMessageAt;
        private String listingTitle;
        private BigDecimal listingRent;
        private String listingAddress;
    }
}
