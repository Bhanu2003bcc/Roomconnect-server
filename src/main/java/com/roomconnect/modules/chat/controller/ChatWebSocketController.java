package com.roomconnect.modules.chat.controller;

import com.roomconnect.modules.chat.service.ChatService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/send")
    public void handleSendMessage(Principal principal, SendMessageRequest req) {
        if (principal == null) {
            throw new IllegalArgumentException("Unauthorized WebSocket client");
        }
        UUID senderId = UUID.fromString(principal.getName());
        chatService.saveAndBroadcastMessage(senderId, req.getConversationId(), req.getBody());
    }

    @Getter @Setter
    public static class SendMessageRequest {
        private UUID conversationId;
        private String body;
    }
}
