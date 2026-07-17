package com.roomconnect.modules.tours;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomconnect.modules.auth.entity.Role;
import com.roomconnect.modules.auth.entity.User;
import com.roomconnect.modules.auth.repository.UserRepository;
import com.roomconnect.modules.auth.service.JwtService;
import com.roomconnect.modules.listings.entity.Category;
import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.modules.tours.controller.SiteVisitController.RequestTourDto;
import com.roomconnect.modules.tours.entity.SiteVisit;
import com.roomconnect.modules.tours.repository.SiteVisitRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
public class SiteVisitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private SiteVisitRepository siteVisitRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User owner;
    private User visitor;
    private Listing listing;
    private String visitorToken;
    private String ownerToken;

    @BeforeEach
    public void setup() {
        siteVisitRepository.deleteAll();
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
    public void testSiteVisitFlow() throws Exception {
        // 1. Request visit
        RequestTourDto req = new RequestTourDto();
        req.setListingId(listing.getId());
        req.setRequestedTime(OffsetDateTime.now().plusDays(2));
        req.setNotes("I want to see the curfew rules.");

        mockMvc.perform(post("/api/tours")
                .header("Authorization", "Bearer " + visitorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("requested"));

        assertFalse(siteVisitRepository.findAll().isEmpty());
        SiteVisit visit = siteVisitRepository.findAll().get(0);

        // 2. Get visitor's tours list
        mockMvc.perform(get("/api/tours/visitor")
                .header("Authorization", "Bearer " + visitorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(visit.getId().toString()))
                .andExpect(jsonPath("$[0].listingTitle").value("Aesthetic room in Noida"));

        // 3. Confirm tour
        mockMvc.perform(patch("/api/tours/" + visit.getId() + "/status")
                .header("Authorization", "Bearer " + ownerToken)
                .param("status", "confirmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("confirmed"));

        SiteVisit updated = siteVisitRepository.findById(visit.getId()).orElseThrow();
        assertEquals(SiteVisit.TourStatus.CONFIRMED, updated.getStatus());
    }
}
