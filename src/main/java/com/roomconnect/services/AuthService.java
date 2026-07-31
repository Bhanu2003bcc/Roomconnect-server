package com.roomconnect.services;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roomconnect.dto.JwtResponse;
import com.roomconnect.dto.OtpSendRequest;
import com.roomconnect.dto.OtpVerifyRequest;
import com.roomconnect.dto.SignupRequest;
import com.roomconnect.dto.SignupResponse;
import com.roomconnect.models.OtpRequest;
import com.roomconnect.models.OwnerProfile;
import com.roomconnect.models.Role;
import com.roomconnect.models.User;
import com.roomconnect.models.VisitorProfile;
import com.roomconnect.repositories.OtpRequestRepository;
import com.roomconnect.repositories.OwnerProfileRepository;
import com.roomconnect.repositories.UserRepository;
import com.roomconnect.repositories.VisitorProfileRepository;
import com.roomconnect.shared.exception.AppException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    
    private final OtpRequestRepository otpRequestRepository;

    private final OwnerProfileRepository ownerProfileRepository;

    private final VisitorProfileRepository visitorProfileRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final NotificationService notificationService;

    private final SecureRandom secureRandom = new SecureRandom(); 

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
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        String codeHash = passwordEncoder.encode(code);

        OtpRequest otpRequest = OtpRequest.builder()
                .user(user)
                .codeHash(codeHash)
                .purpose("LOGIN_OR_VERIFY")
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        otpRequestRepository.save(otpRequest);

        log.info("--------------------------------------------------");
        log.info("OTP Code for user {} is: {}", user.getPhone(), code);
        log.info("--------------------------------------------------");
        
        notificationService.sendSms(
                user.getPhone(),
                "Your Room2Live OTP is: " + code + ". Valid for 10 minutes. Do not share this code."
        );
    }

    @Transactional
    public JwtResponse verifyOtp(OtpVerifyRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AppException("Phone number not registered", HttpStatus.NOT_FOUND));

        if ("suspended".equals(user.getStatus())) {
            throw new AppException("User account is suspended", HttpStatus.FORBIDDEN);
        }


         // Fixed Optimization: Fetch only the single latest unconsumed record to save heavy CPU processing
        OtpRequest latestOtp = otpRequestRepository
                .findFirstByUserAndPurposeAndConsumedAtIsNullAndExpiresAtAfterOrderByIdDesc(
                        user, "LOGIN_OR_VERIFY", OffsetDateTime.now())
                .orElseThrow(() -> new AppException("Invalid or expired OTP code", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getCode(), latestOtp.getCodeHash())) {
            throw new AppException("Invalid or expired OTP code", HttpStatus.UNAUTHORIZED);
        }

        latestOtp.setConsumedAt(OffsetDateTime.now());
        otpRequestRepository.save(latestOtp);

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
