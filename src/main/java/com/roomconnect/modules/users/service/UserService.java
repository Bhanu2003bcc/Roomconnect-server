package com.roomconnect.modules.users.service;

import com.roomconnect.modules.auth.entity.Role;
import com.roomconnect.modules.auth.entity.User;
import com.roomconnect.modules.auth.repository.UserRepository;
import com.roomconnect.modules.users.dto.UserProfileDto;
import com.roomconnect.modules.users.entity.OwnerProfile;
import com.roomconnect.modules.users.entity.VisitorProfile;
import com.roomconnect.modules.users.repository.OwnerProfileRepository;
import com.roomconnect.modules.users.repository.VisitorProfileRepository;
import com.roomconnect.shared.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OwnerProfileRepository ownerProfileRepository;

    @Autowired
    private VisitorProfileRepository visitorProfileRepository;

    public UserProfileDto getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        UserProfileDto.UserProfileDtoBuilder builder = UserProfileDto.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole());

        if (user.getRole() == Role.owner) {
            OwnerProfile profile = ownerProfileRepository.findById(userId)
                    .orElseThrow(() -> new AppException("Owner profile not found", HttpStatus.NOT_FOUND));
            builder.fullName(profile.getFullName())
                   .address(profile.getAddress())
                   .landmark(profile.getLandmark())
                   .cityId(profile.getCityId());
        } else if (user.getRole() == Role.visitor) {
            VisitorProfile profile = visitorProfileRepository.findById(userId)
                    .orElseThrow(() -> new AppException("Visitor profile not found", HttpStatus.NOT_FOUND));
            builder.fullName(profile.getFullName())
                   .homeAddress(profile.getHomeAddress())
                   .profession(profile.getProfession())
                   .cityId(profile.getCityId());
        }

        return builder.build();
    }

    @Transactional
    public UserProfileDto updateUserProfile(UUID userId, UserProfileDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (dto.getEmail() != null && !dto.getEmail().isBlank() && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new AppException("Email already registered by another user", HttpStatus.CONFLICT);
            }
            user.setEmail(dto.getEmail());
            userRepository.save(user);
        }

        if (user.getRole() == Role.owner) {
            OwnerProfile profile = ownerProfileRepository.findById(userId)
                    .orElseThrow(() -> new AppException("Owner profile not found", HttpStatus.NOT_FOUND));
            if (dto.getFullName() != null) profile.setFullName(dto.getFullName());
            if (dto.getAddress() != null) profile.setAddress(dto.getAddress());
            if (dto.getLandmark() != null) profile.setLandmark(dto.getLandmark());
            if (dto.getCityId() != null) profile.setCityId(dto.getCityId());
            ownerProfileRepository.save(profile);
        } else if (user.getRole() == Role.visitor) {
            VisitorProfile profile = visitorProfileRepository.findById(userId)
                    .orElseThrow(() -> new AppException("Visitor profile not found", HttpStatus.NOT_FOUND));
            if (dto.getFullName() != null) profile.setFullName(dto.getFullName());
            if (dto.getHomeAddress() != null) profile.setHomeAddress(dto.getHomeAddress());
            if (dto.getProfession() != null) profile.setProfession(dto.getProfession());
            if (dto.getCityId() != null) profile.setCityId(dto.getCityId());
            visitorProfileRepository.save(profile);
        }

        return getUserProfile(userId);
    }
}
