package com.roomconnect.modules.listings.controller;

import com.roomconnect.modules.listings.dto.CreateListingRequest;
import com.roomconnect.modules.listings.dto.ListingResponse;
import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.entity.ListingStatus;
import com.roomconnect.modules.listings.service.ListingService;
import com.roomconnect.modules.media.service.MediaService;
import com.roomconnect.shared.config.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final MediaService mediaService;
    private final JwtUtil jwtUtil;

    /** POST /api/listings — owner only */
    @PostMapping
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<ListingResponse> create(
            @AuthenticationPrincipal UUID ownerId,
            @Valid @RequestBody CreateListingRequest req) {
        Listing listing = listingService.createListing(ownerId, req);
        return ResponseEntity.ok(toResponse(listing));
    }

    /** GET /api/listings/{id} — public */
    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getById(@PathVariable UUID id) {
        Listing listing = listingService.getById(id);
        return ResponseEntity.ok(toResponseWithMedia(listing));
    }

    /** PUT /api/listings/{id} — owner only, ownership enforced in service */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<ListingResponse> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID requesterId,
            @Valid @RequestBody CreateListingRequest req) {
        Listing listing = listingService.updateListing(id, requesterId, req);
        return ResponseEntity.ok(toResponse(listing));
    }

    /** PATCH /api/listings/{id}/status — owner only */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID requesterId,
            @RequestParam String status) {
        listingService.updateStatus(id, requesterId, ListingStatus.valueOf(status.toUpperCase()));
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }

    /** DELETE /api/listings/{id} — owner only */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID requesterId) {
        listingService.deleteListing(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/listings/my — owner's own listings */
    @GetMapping("/my")
    @PreAuthorize("hasRole('owner')")
    public ResponseEntity<Page<ListingResponse>> myListings(
            @AuthenticationPrincipal UUID ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Listing> listings = listingService.getOwnerListings(ownerId, page, size);
        return ResponseEntity.ok(listings.map(this::toResponseWithMedia));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ListingResponse toResponse(Listing l) {
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
        return r;
    }

    private ListingResponse toResponseWithMedia(Listing l) {
        ListingResponse r = toResponse(l);
        List<ListingResponse.MediaItem> mediaItems = mediaService.getListingMedia(l.getId())
                .stream()
                .map(m -> {
                    ListingResponse.MediaItem item = new ListingResponse.MediaItem();
                    item.setId(m.getId());
                    item.setUrl(m.getFileKey()); // resolved to CDN URL in client
                    item.setThumbnailUrl(m.getThumbnailKey());
                    item.setSortOrder(m.getSortOrder());
                    return item;
                })
                .toList();
        r.setMedia(mediaItems);
        return r;
    }
}
