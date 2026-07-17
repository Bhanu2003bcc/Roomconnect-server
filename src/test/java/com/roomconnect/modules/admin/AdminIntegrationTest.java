package com.roomconnect.modules.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomconnect.modules.admin.repository.AuditLogRepository;
import com.roomconnect.modules.auth.entity.Role;
import com.roomconnect.modules.auth.entity.User;
import com.roomconnect.modules.auth.repository.UserRepository;
import com.roomconnect.modules.auth.service.JwtService;
import com.roomconnect.modules.listings.entity.Category;
import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.repository.ListingRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
public class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User admin;
    private User targetUser;
    private String adminToken;

    @BeforeEach
    public void setup() {
        auditLogRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        admin = User.builder()
                .phone("+919999999999")
                .role(Role.admin)
                .phoneVerified(true)
                .build();
        admin = userRepository.save(admin);

        targetUser = User.builder()
                .phone("+911111111111")
                .role(Role.owner)
                .phoneVerified(true)
                .status("active")
                .build();
        targetUser = userRepository.save(targetUser);

        adminToken = jwtService.generateAccessToken(admin.getId(), admin.getPhone(), Role.admin.name());
    }

    @Test
    public void testAdminActions() throws Exception {
        // 1. Get metrics
        mockMvc.perform(get("/api/admin/metrics")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.totalListings").value(0));

        // 2. Suspend target user
        mockMvc.perform(post("/api/admin/users/" + targetUser.getId() + "/suspend")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        User reloaded = userRepository.findById(targetUser.getId()).orElseThrow();
        assertEquals("suspended", reloaded.getStatus());

        // 3. View audit logs
        mockMvc.perform(get("/api/admin/audit-logs")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("SUSPEND_USER"));
    }
}
