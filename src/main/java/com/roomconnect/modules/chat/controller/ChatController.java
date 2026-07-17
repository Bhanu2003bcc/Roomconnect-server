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
        List<ConversationResponse> response = convs.stream().map(c -> {
            Listing listing = listingRepository.findById(c.getListingId()).orElse(null);
            ConversationResponse r = new ConversationResponse();
            r.setId(c.getId());
            r.setListingId(c.getListingId());
            r.setOwnerId(c.getOwnerId());
            r.setVisitorId(c.getVisitorId());
            r.setCreatedAt(c.getCreatedAt());
            r.setLastMessageAt(c.getLastMessageAt());
            if (listing != null) {
                r.setListingTitle(listing.getTitle());
                r.setListingRent(listing.getRentAmount());
                r.setListingAddress(listing.getAddressText());
            }
            return r;
        }).collect(Collectors.toList());
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

    /** POST /api/chat/conversations — Create or get conversation for a listing */
    @PostMapping("/conversations")
    public ResponseEntity<Conversation> startConversation(
            @RequestParam UUID listingId,
            @AuthenticationPrincipal UUID currentUserId) {
        Conversation conv = chatService.getOrCreateConversation(listingId, currentUserId);
        return ResponseEntity.ok(conv);
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
