package com.roomconnect.modules.admin.service;

import com.roomconnect.modules.admin.dto.AdminCreateUserRequest;
import com.roomconnect.modules.admin.dto.AdminUserDto;
import com.roomconnect.modules.admin.entity.AuditLog;
import com.roomconnect.modules.admin.repository.AuditLogRepository;
import com.roomconnect.modules.auth.entity.Role;
import com.roomconnect.modules.auth.entity.User;
import com.roomconnect.modules.auth.repository.UserRepository;
import com.roomconnect.modules.listings.dto.ListingResponse;
import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.entity.ListingStatus;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.modules.media.service.MediaService;
import com.roomconnect.modules.chat.repository.MessageRepository;
import com.roomconnect.modules.users.entity.OwnerProfile;
import com.roomconnect.modules.users.entity.VisitorProfile;
import com.roomconnect.modules.users.repository.OwnerProfileRepository;
import com.roomconnect.modules.users.repository.VisitorProfileRepository;
import com.roomconnect.shared.exception.AppException;
import com.roomconnect.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final MessageRepository messageRepository;
    private final AuditLogRepository auditLogRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final VisitorProfileRepository visitorProfileRepository;
    private final MediaService mediaService;
    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucket;

    @Transactional(readOnly = true)
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalUsers", userRepository.count());
        metrics.put("totalListings", listingRepository.count());
        metrics.put("totalMessages", messageRepository.count());
        return metrics;
    }

    @Transactional
    public void suspendUser(UUID adminId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setStatus("suspended");
        userRepository.save(user);

        logAudit(adminId, "SUSPEND_USER", "USER", userId, Map.of("phone", user.getPhone()));
        log.info("Admin {} suspended user {}", adminId, userId);
    }

    @Transactional
    public void unsuspendUser(UUID adminId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setStatus("active");
        userRepository.save(user);

        logAudit(adminId, "UNSUSPEND_USER", "USER", userId, Map.of("phone", user.getPhone()));
        log.info("Admin {} unsuspended user {}", adminId, userId);
    }

    @Transactional
    public void suspendListing(UUID adminId, UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));
        listing.setStatus(ListingStatus.OCCUPIED);
        listingRepository.save(listing);

        logAudit(adminId, "SUSPEND_LISTING", "LISTING", listingId, Map.of("title", listing.getTitle()));
        log.info("Admin {} suspended listing {}", adminId, listingId);
    }

    @Transactional
    public void deleteListing(UUID adminId, UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));
        listingRepository.delete(listing);

        logAudit(adminId, "DELETE_LISTING", "LISTING", listingId, Map.of("title", listing.getTitle()));
        log.info("Admin {} deleted listing {}", adminId, listingId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, List<AdminUserDto>> getUsersGroupedByRole() {
        List<User> users = userRepository.findAll();
        List<OwnerProfile> owners = ownerProfileRepository.findAll();
        List<VisitorProfile> visitors = visitorProfileRepository.findAll();

        Map<UUID, String> ownerNames = owners.stream()
                .collect(Collectors.toMap(OwnerProfile::getUserId, OwnerProfile::getFullName, (a, b) -> a));
        Map<UUID, String> visitorNames = visitors.stream()
                .collect(Collectors.toMap(VisitorProfile::getUserId, VisitorProfile::getFullName, (a, b) -> a));

        List<AdminUserDto> adminsList = new ArrayList<>();
        List<AdminUserDto> ownersList = new ArrayList<>();
        List<AdminUserDto> visitorsList = new ArrayList<>();

        for (User u : users) {
            String name = "";
            if (u.getRole() == Role.owner) {
                name = ownerNames.getOrDefault(u.getId(), "");
            } else if (u.getRole() == Role.visitor) {
                name = visitorNames.getOrDefault(u.getId(), "");
            }

            AdminUserDto dto = AdminUserDto.builder()
                    .id(u.getId())
                    .phone(u.getPhone())
                    .email(u.getEmail())
                    .role(u.getRole().name())
                    .status(u.getStatus())
                    .fullName(name)
                    .createdAt(u.getCreatedAt())
                    .build();

            if (u.getRole() == Role.admin) {
                adminsList.add(dto);
            } else if (u.getRole() == Role.owner) {
                ownersList.add(dto);
            } else if (u.getRole() == Role.visitor) {
                visitorsList.add(dto);
            }
        }

        Map<String, List<AdminUserDto>> result = new HashMap<>();
        result.put("admins", adminsList);
        result.put("owners", ownersList);
        result.put("visitors", visitorsList);
        return result;
    }

    @Transactional
    public void deleteUser(UUID adminId, UUID userId) {
        if (adminId.equals(userId)) {
            throw new AppException("Self-deletion is forbidden for security", HttpStatus.BAD_REQUEST);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Find and delete listing files from R2/S3
        List<Listing> userListings = listingRepository.findAll().stream()
                .filter(l -> l.getOwnerId().equals(userId))
                .toList();

        for (Listing l : userListings) {
            try {
                mediaService.getListingMedia(l.getId()).forEach(m -> {
                    try {
                        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(m.getFileKey()).build());
                        if (m.getThumbnailKey() != null) {
                            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(m.getThumbnailKey()).build());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to delete media file from R2/S3: {}", e.getMessage());
                    }
                });
            } catch (Exception e) {
                log.warn("Failed to retrieve or delete media for listing {}: {}", l.getId(), e.getMessage());
            }
        }

        userRepository.delete(user);
        logAudit(adminId, "DELETE_USER", "USER", userId, Map.of("phone", user.getPhone()));
        log.info("Admin {} deleted user {} and all their content", adminId, userId);
    }

    @Transactional
    public User createUser(UUID adminId, AdminCreateUserRequest req) {
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new AppException("Phone number already registered", HttpStatus.CONFLICT);
        }
        if (req.getEmail() != null && !req.getEmail().isBlank() && userRepository.existsByEmail(req.getEmail())) {
            throw new AppException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .phone(req.getPhone())
                .email(req.getEmail() != null && !req.getEmail().isBlank() ? req.getEmail() : null)
                .role(req.getRole())
                .phoneVerified(true)
                .emailVerified(req.getEmail() != null && !req.getEmail().isBlank())
                .status(req.getStatus() != null ? req.getStatus() : "active")
                .consentAt(OffsetDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        if (req.getRole() == Role.owner) {
            OwnerProfile ownerProfile = OwnerProfile.builder()
                    .userId(savedUser.getId())
                    .fullName(req.getFullName())
                    .cityId(1)
                    .build();
            ownerProfileRepository.save(ownerProfile);
        } else if (req.getRole() == Role.visitor) {
            VisitorProfile visitorProfile = VisitorProfile.builder()
                    .userId(savedUser.getId())
                    .fullName(req.getFullName())
                    .cityId(1)
                    .build();
            visitorProfileRepository.save(visitorProfile);
        }

        logAudit(adminId, "ADD_USER", "USER", savedUser.getId(), Map.of("phone", savedUser.getPhone(), "role", savedUser.getRole().name()));
        log.info("Admin {} created user {}", adminId, savedUser.getId());
        return savedUser;
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getAllListings() {
        List<Listing> listings = listingRepository.findAll();
        return listings.stream().map(this::toResponseWithMedia).toList();
    }

    private ListingResponse toResponseWithMedia(Listing l) {
        ListingResponse r = new ListingResponse();
        r.setId(l.getId());
        r.setOwnerId(l.getOwnerId());
        r.setCityId(l.getCityId());
        r.setCategory(l.getCategory());
        r.setTitle(l.getTitle());
        r.setDescription(l.getDescription());
        r.setRentAmount(l.getRentAmount());
        r.setDepositAmount(l.getDepositAmount());
        r.setBathroomType(l.getBathroomType());
        r.setFurnishing(l.getFurnishing());
        r.setGenderPreference(l.getGenderPreference());
        r.setFoodIncluded(l.isFoodIncluded());
        r.setFoodType(l.getFoodType());
        r.setCurfewTime(l.getCurfewTime());
        r.setAc(l.getAc());
        r.setWifi(l.isWifi());
        r.setParking(l.isParking());
        r.setLaundry(l.isLaundry());
        r.setAddressText(l.getAddressText());
        r.setLatitude(l.getLatitude());
        r.setLongitude(l.getLongitude());
        r.setStatus(l.getStatus());
        r.setAvailableFromDate(l.getAvailableFromDate());
        r.setCreatedAt(l.getCreatedAt());
        r.setUpdatedAt(l.getUpdatedAt());

        try {
            List<ListingResponse.MediaItem> mediaItems = mediaService.getListingMedia(l.getId())
                    .stream()
                    .map(m -> {
                        ListingResponse.MediaItem item = new ListingResponse.MediaItem();
                        item.setId(m.getId());
                        item.setUrl(mediaService.getPublicUrl(m.getFileKey()));
                        item.setThumbnailUrl(mediaService.getPublicUrl(m.getThumbnailKey()));
                        item.setSortOrder(m.getSortOrder());
                        return item;
                    })
                    .toList();
            r.setMedia(mediaItems);
        } catch (Exception e) {
            log.warn("Failed to load media for listing {}: {}", l.getId(), e.getMessage());
        }
        return r;
    }

    private void logAudit(UUID adminId, String action, String targetType, UUID targetId, Map<String, Object> meta) {
        AuditLog logEntry = AuditLog.builder()
                .adminId(adminId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .build();
        logEntry.setMetadataMap(meta);
        auditLogRepository.save(logEntry);
    }
}
