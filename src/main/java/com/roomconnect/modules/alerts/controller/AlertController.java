package com.roomconnect.modules.alerts.controller;

import com.roomconnect.modules.alerts.entity.SavedSearch;
import com.roomconnect.modules.alerts.service.AlertService;
import com.roomconnect.modules.search.dto.SearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /** POST /api/alerts — Save a search filter set */
    @PostMapping
    @PreAuthorize("hasRole('visitor')")
    public ResponseEntity<SavedSearch> saveSearch(
            @AuthenticationPrincipal UUID currentUserId,
            @RequestBody SearchRequest filters) {
        SavedSearch ss = alertService.saveSearch(currentUserId, filters);
        return ResponseEntity.status(HttpStatus.CREATED).body(ss);
    }

    /** GET /api/alerts — List visitor's saved searches */
    @GetMapping
    @PreAuthorize("hasRole('visitor')")
    public ResponseEntity<List<SavedSearch>> getSavedSearches(@AuthenticationPrincipal UUID currentUserId) {
        List<SavedSearch> searches = alertService.getVisitorSearches(currentUserId);
        return ResponseEntity.ok(searches);
    }

    /** DELETE /api/alerts/{id} — Delete a saved search */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('visitor')")
    public ResponseEntity<Void> deleteSearch(
             @PathVariable UUID id,
             @AuthenticationPrincipal UUID currentUserId) {
        alertService.deleteSearch(currentUserId, id);
        return ResponseEntity.noContent().build();
    }
}
