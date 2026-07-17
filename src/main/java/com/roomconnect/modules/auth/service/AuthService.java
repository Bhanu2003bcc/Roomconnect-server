package com.roomconnect.modules.auth.service;

import com.roomconnect.modules.auth.dto.*;
import com.roomconnect.modules.auth.entity.OtpRequest;
import com.roomconnect.modules.auth.entity.Role;
import com.roomconnect.modules.auth.entity.User;
import com.roomconnect.modules.auth.repository.OtpRequestRepository;
import com.roomconnect.modules.auth.repository.UserRepository;
import com.roomconnect.modules.notifications.service.NotificationService;
import com.roomconnect.modules.users.entity.OwnerProfile;
import com.roomconnect.modules.users.entity.VisitorProfile;
import com.roomconnect.modules.users.repository.OwnerProfileRepository;
import com.roomconnect.modules.users.repository.VisitorProfileRepository;
import com.roomconnect.shared.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpRequestRepository otpRequestRepository;

    @Autowired
    private OwnerProfileRepository ownerProfileRepository;

    @Autowired
    private VisitorProfileRepository visitorProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private NotificationService notificationService;

    private final Random random = new Random();

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AppException("Phone number already registered", HttpStatus.CONFLICT);
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail() : null)
                .role(request.getRole())
                .phoneVerified(false)
                .emailVerified(false)
                .status("active")
                .consentAt(request.getConsent() ? OffsetDateTime.now() : null)
                .build();

        User savedUser = userRepository.save(user);

        if (request.getRole() == Role.owner) {
            OwnerProfile ownerProfile = OwnerProfile.builder()
                    .userId(savedUser.getId())
                    .fullName(request.getFullName())
                    .cityId(1) // default Noida
                    .build();
            ownerProfileRepository.save(ownerProfile);
        } else if (request.getRole() == Role.visitor) {
            VisitorProfile visitorProfile = VisitorProfile.builder()
                    .userId(savedUser.getId())
                    .fullName(request.getFullName())
                    .cityId(1) // default Noida
                    .build();
            visitorProfileRepository.save(visitorProfile);
        }

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getPhone(),
                savedUser.getRole(),
                "Registration successful. Please request OTP to verify your phone number."
        );
    }

    public void sendOtp(OtpSendRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AppException("Phone number not registered", HttpStatus.NOT_FOUND));

        if ("suspended".equals(user.getStatus())) {
            throw new AppException("User account is suspended", HttpStatus.FORBIDDEN);
        }

        // Generate 6-digit OTP code
        String code = String.format("%06d", random.nextInt(1000000));
        String codeHash = passwordEncoder.encode(code);

        OtpRequest otpRequest = OtpRequest.builder()
                .user(user)
                .codeHash(codeHash)
                .purpose("LOGIN_OR_VERIFY")
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        otpRequestRepository.save(otpRequest);

        // DEV: also log to terminal for quick testing
        log.info("--------------------------------------------------");
        log.info("OTP Code for user {} is: {}", user.getPhone(), code);
        log.info("--------------------------------------------------");

        // Send OTP via SMS (Twilio in production, mock-logged in dev)
        notificationService.sendSms(
                user.getPhone(),
                "Your RoomConnect OTP is: " + code + ". Valid for 10 minutes. Do not share this code."
        );
    }

    @Transactional
    public JwtResponse verifyOtp(OtpVerifyRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AppException("Phone number not registered", HttpStatus.NOT_FOUND));

        if ("suspended".equals(user.getStatus())) {
            throw new AppException("User account is suspended", HttpStatus.FORBIDDEN);
        }

        List<OtpRequest> validOtps = otpRequestRepository
                .findByUserAndPurposeAndConsumedAtIsNullAndExpiresAtAfter(user, "LOGIN_OR_VERIFY", OffsetDateTime.now());

        OtpRequest matchingOtp = null;
        for (OtpRequest otp : validOtps) {
            if (passwordEncoder.matches(request.getCode(), otp.getCodeHash())) {
                matchingOtp = otp;
                break;
            }
        }

        if (matchingOtp == null) {
            throw new AppException("Invalid or expired OTP code", HttpStatus.UNAUTHORIZED);
        }

        // Consume OTP
        matchingOtp.setConsumedAt(OffsetDateTime.now());
        otpRequestRepository.save(matchingOtp);

        // Verify Phone on user
        if (!user.isPhoneVerified()) {
            user.setPhoneVerified(true);
            userRepository.save(user);
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getPhone(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return new JwtResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getRole(),
                user.getPhone()
        );
    }
}
