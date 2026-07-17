package com.roomconnect.modules.admin.controller;

import com.roomconnect.modules.admin.entity.AuditLog;
import com.roomconnect.modules.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('admin')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** GET /api/admin/metrics — Get platform usage counts */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(adminService.getMetrics());
    }

    /** POST /api/admin/users/{userId}/suspend — Suspend a user account */
    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<Map<String, String>> suspendUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UUID adminId) {
        adminService.suspendUser(adminId, userId);
        return ResponseEntity.ok(Map.of("message", "User suspended"));
    }

    /** POST /api/admin/users/{userId}/unsuspend — Reactivate user account */
    @PostMapping("/users/{userId}/unsuspend")
    public ResponseEntity<Map<String, String>> unsuspendUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UUID adminId) {
        adminService.unsuspendUser(adminId, userId);
        return ResponseEntity.ok(Map.of("message", "User activated"));
    }

    /** POST /api/admin/listings/{listingId}/suspend — Hide a listing */
    @PostMapping("/listings/{listingId}/suspend")
    public ResponseEntity<Map<String, String>> suspendListing(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal UUID adminId) {
        adminService.suspendListing(adminId, listingId);
        return ResponseEntity.ok(Map.of("message", "Listing status set to OCCUPIED"));
    }

    /** DELETE /api/admin/listings/{listingId} — Permanently remove listing */
    @DeleteMapping("/listings/{listingId}")
    public ResponseEntity<Void> deleteListing(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal UUID adminId) {
        adminService.deleteListing(adminId, listingId);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/admin/audit-logs — View system audit trails */
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAuditLogs(page, size));
    }
}
