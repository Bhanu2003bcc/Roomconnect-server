package com.roomconnect.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomconnect.modules.auth.dto.*;
import com.roomconnect.modules.auth.entity.OtpRequest;
import com.roomconnect.modules.auth.entity.Role;
import com.roomconnect.modules.auth.entity.User;
import com.roomconnect.modules.auth.repository.OtpRequestRepository;
import com.roomconnect.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ImportAutoConfiguration(exclude = {
    org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration.class,
    org.jobrunr.spring.autoconfigure.storage.JobRunrSqlStorageAutoConfiguration.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpRequestRepository otpRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        otpRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testFullAuthenticationFlow() throws Exception {
        // 1. Signup Request
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setPhone("+919999999999");
        signupRequest.setEmail("visitor@roomconnect.com");
        signupRequest.setFullName("Test Visitor");
        signupRequest.setRole(Role.visitor);
        signupRequest.setConsent(true);

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        assertTrue(userRepository.existsByPhone("+919999999999"));

        // 2. Request OTP
        OtpSendRequest sendRequest = new OtpSendRequest();
        sendRequest.setPhone("+919999999999");

        mockMvc.perform(post("/api/auth/otp/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk());

        // Extract code from DB and set a known code hash to avoid slow brute-force matching
        User user = userRepository.findByPhone("+919999999999").orElseThrow();
        List<OtpRequest> otps = otpRequestRepository.findAll();
        assertFalse(otps.isEmpty());
        
        OtpRequest otp = otps.get(0);
        String correctCode = "123456";
        otp.setCodeHash(passwordEncoder.encode(correctCode));
        otpRequestRepository.save(otp);

        OtpVerifyRequest verifyRequest = new OtpVerifyRequest();
        verifyRequest.setPhone("+919999999999");
        verifyRequest.setCode(correctCode);

        MvcResult result = mockMvc.perform(post("/api/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(responseBody, JwtResponse.class);

        assertNotNull(jwtResponse.getAccessToken());
        assertNotNull(jwtResponse.getRefreshToken());
        assertEquals(user.getId(), jwtResponse.getUserId());
        assertEquals(Role.visitor, jwtResponse.getRole());
        
        // Reload user to verify phone_verified is now true
        User reloadedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(reloadedUser.isPhoneVerified());
    }
}
