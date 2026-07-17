package com.roomconnect.modules.admin.service;

import com.roomconnect.modules.admin.entity.AuditLog;
import com.roomconnect.modules.admin.repository.AuditLogRepository;
import com.roomconnect.modules.auth.entity.User;
import com.roomconnect.modules.auth.repository.UserRepository;
import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.entity.ListingStatus;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.modules.chat.repository.MessageRepository;
import com.roomconnect.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final MessageRepository messageRepository;
    private final AuditLogRepository auditLogRepository;

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
