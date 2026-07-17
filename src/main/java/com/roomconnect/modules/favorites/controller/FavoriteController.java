package com.roomconnect.modules.favorites.controller;

import com.roomconnect.modules.favorites.entity.Favorite;
import com.roomconnect.modules.favorites.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** POST /api/favorites/{listingId} — toggle favorite */
    @PostMapping("/{listingId}")
    @PreAuthorize("hasRole('visitor')")
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal UUID visitorId) {
        boolean favorited = favoriteService.toggle(visitorId, listingId);
        return ResponseEntity.ok(Map.of(
                "listingId", listingId,
                "favorited", favorited
        ));
    }

    /** GET /api/favorites — paginated saved listings */
    @GetMapping
    @PreAuthorize("hasRole('visitor')")
    public ResponseEntity<Page<Favorite>> list(
            @AuthenticationPrincipal UUID visitorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(favoriteService.getFavorites(visitorId, page, size));
    }

    /** GET /api/favorites/{listingId}/status */
    @GetMapping("/{listingId}/status")
    @PreAuthorize("hasRole('visitor')")
    public ResponseEntity<Map<String, Boolean>> status(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal UUID visitorId) {
        return ResponseEntity.ok(Map.of("favorited", favoriteService.isFavorited(visitorId, listingId)));
    }
}
