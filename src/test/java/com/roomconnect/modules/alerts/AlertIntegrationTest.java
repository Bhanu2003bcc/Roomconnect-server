package com.roomconnect.modules.alerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomconnect.models.SavedSearch;
import com.roomconnect.repositories.SavedSearchRepository;
import com.roomconnect.services.SavedSearchAlertWorker;
import com.roomconnect.models.Role;
import com.roomconnect.models.User;
import com.roomconnect.repositories.UserRepository;
import com.roomconnect.services.JwtService;
import com.roomconnect.dto.SearchRequest;
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
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
public class AlertIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SavedSearchRepository savedSearchRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private SavedSearchAlertWorker alertWorker;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User visitor;
    private User owner;
    private String visitorToken;

    @BeforeEach
    public void setup() {
        savedSearchRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        visitor = User.builder()
                .phone("+912222222222")
                .email("visitor@roomconnect.com")
                .role(Role.visitor)
                .phoneVerified(true)
                .build();
        visitor = userRepository.save(visitor);

        owner = User.builder()
                .phone("+911111111111")
                .role(Role.owner)
                .phoneVerified(true)
                .build();
        owner = userRepository.save(owner);

        visitorToken = jwtService.generateAccessToken(visitor.getId(), visitor.getPhone(), Role.visitor.name());
    }

    @Test
    public void testAlertFlow() throws Exception {
        // 1. Create alert request
        SearchRequest filters = new SearchRequest();
        filters.setLat(28.62);
        filters.setLng(77.36);
        filters.setRadiusKm(5.0);
        filters.setCategory(Category.PG);

        mockMvc.perform(post("/api/alerts")
                .header("Authorization", "Bearer " + visitorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filters)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        assertFalse(savedSearchRepository.findAll().isEmpty());
        SavedSearch ss = savedSearchRepository.findAll().get(0);

        // 2. Add a new listing that matches
        Listing newListing = Listing.builder()
                .ownerId(owner.getId()) // real owner ID
                .title("Premium Noida PG")
                .category(Category.PG)
                .rentAmount(BigDecimal.valueOf(14000))
                .addressText("Sector 62, Noida")
                .latitude(28.62)
                .longitude(77.36)
                .createdAt(OffsetDateTime.now().plusSeconds(1)) // Ensure it is created after the alert
                .build();
        listingRepository.save(newListing);

        // 3. Trigger alert worker to process alerts
        alertWorker.processAlerts();

        // 4. Delete alert
        mockMvc.perform(delete("/api/alerts/" + ss.getId())
                .header("Authorization", "Bearer " + visitorToken))
                .andExpect(status().isNoContent());

        assertTrue(savedSearchRepository.findAll().isEmpty());
    }
}
