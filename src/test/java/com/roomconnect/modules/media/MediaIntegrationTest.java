package com.roomconnect.modules.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomconnect.modules.auth.entity.Role;
import com.roomconnect.modules.auth.entity.User;
import com.roomconnect.modules.auth.repository.UserRepository;
import com.roomconnect.modules.auth.service.JwtService;
import com.roomconnect.modules.listings.entity.Category;
import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.modules.media.repository.ListingMediaRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
public class MediaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingMediaRepository mediaRepository;

    @Autowired
    private JwtService jwtService;

    private User owner;
    private Listing listing;
    private String ownerToken;

    @BeforeEach
    public void setup() {
        mediaRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        owner = User.builder()
                .phone("+911111111111")
                .role(Role.owner)
                .phoneVerified(true)
                .build();
        owner = userRepository.save(owner);

        ownerToken = jwtService.generateAccessToken(owner.getId(), owner.getPhone(), Role.owner.name());

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
    public void testPresignAndConfirmFlow() throws Exception {
        // 1. Get presigned URL
        String presignResultJson = mockMvc.perform(post("/api/listings/" + listing.getId() + "/media/presign")
                .header("Authorization", "Bearer " + ownerToken)
                .param("mimeType", "image/png")
                .param("sizeBytes", "1024"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaId").exists())
                .andExpect(jsonPath("$.fileKey").exists())
                .andExpect(jsonPath("$.uploadUrl").value(containsString("http://localhost:9000/mock-bucket/listings/")))
                .andReturn().getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        var node = mapper.readTree(presignResultJson);
        UUID mediaId = UUID.fromString(node.get("mediaId").asText());

        // 2. Confirm upload
        mockMvc.perform(post("/api/listings/" + listing.getId() + "/media/" + mediaId + "/confirm")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Upload confirmed")));

        var media = mediaRepository.findById(mediaId).orElseThrow();
        assertTrue(media.getProcessingStatus().equals("done"));
    }
}
