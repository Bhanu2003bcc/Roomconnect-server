package com.roomconnect.modules.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomconnect.models.Role;
import com.roomconnect.models.User;
import com.roomconnect.repositories.UserRepository;
import com.roomconnect.services.JwtService;
import com.roomconnect.models.Conversation;
import com.roomconnect.repositories.ConversationRepository;
import com.roomconnect.repositories.MessageRepository;
import com.roomconnect.models.Category;
import com.roomconnect.models.Listing;
import com.roomconnect.repositories.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ImportAutoConfiguration(exclude = {
    org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration.class,
    org.jobrunr.spring.autoconfigure.storage.JobRunrSqlStorageAutoConfiguration.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User owner;
    private User visitor;
    private Listing listing;
    private String visitorToken;

    @BeforeEach
    public void setup() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        owner = User.builder()
                .phone("+911111111111")
                .role(Role.owner)
                .phoneVerified(true)
                .build();
        owner = userRepository.save(owner);

        visitor = User.builder()
                .phone("+912222222222")
                .role(Role.visitor)
                .phoneVerified(true)
                .build();
        visitor = userRepository.save(visitor);

        visitorToken = jwtService.generateAccessToken(visitor.getId(), visitor.getPhone(), Role.visitor.name());

        listing = Listing.builder()
                .ownerId(owner.getId())
                .title("Aesthetic room in Noida")
                .category(Category.PG)
                .rentAmount(BigDecimal.valueOf(12000))
                .addressText("Sector 62, Noida")
                .latitude(28.62)
                .longitude(77.36)
                .build();
        listing = listingRepository.save(listing);
    }

    @Test
    public void testConversationFlow() throws Exception {
        // 1. Create or get conversation
        mockMvc.perform(post("/api/chat/conversations")
                .header("Authorization", "Bearer " + visitorToken)
                .param("listingId", listing.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value(listing.getId().toString()))
                .andExpect(jsonPath("$.visitorId").value(visitor.getId().toString()))
                .andExpect(jsonPath("$.ownerId").value(owner.getId().toString()));

        assertFalse(conversationRepository.findAll().isEmpty());

        Conversation conv = conversationRepository.findAll().get(0);

        // 2. Fetch conversations list
        mockMvc.perform(get("/api/chat/conversations")
                .header("Authorization", "Bearer " + visitorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conv.getId().toString()))
                .andExpect(jsonPath("$[0].listingTitle").value("Aesthetic room in Noida"));

        // 3. Fetch messages (should be empty page)
        mockMvc.perform(get("/api/chat/conversations/" + conv.getId() + "/messages")
                .header("Authorization", "Bearer " + visitorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    public void testListingDeletionPreservesConversationsFor90Days() throws Exception {
        // 1. Create conversation
        mockMvc.perform(post("/api/chat/conversations")
                .header("Authorization", "Bearer " + visitorToken)
                .param("listingId", listing.getId().toString()))
                .andExpect(status().isCreated());

        // 2. Delete the listing
        listingRepository.delete(listing);

        // 3. Verify conversation still exists and returns snapshot details
        mockMvc.perform(get("/api/chat/conversations")
                .header("Authorization", "Bearer " + visitorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].listingTitle").value("Aesthetic room in Noida"));
    }
}
