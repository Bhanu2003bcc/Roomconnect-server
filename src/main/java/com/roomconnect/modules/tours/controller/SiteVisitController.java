package com.roomconnect.modules.tours.controller;

import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.modules.tours.entity.SiteVisit;
import com.roomconnect.modules.tours.entity.SiteVisit.TourStatus;
import com.roomconnect.modules.tours.service.SiteVisitService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class SiteVisitController {

    private final SiteVisitService siteVisitService;
    private final ListingRepository listingRepository;

    /** POST /api/tours — Request a new site visit (visitor only) */
    @PostMapping
    public ResponseEntity<SiteVisit> requestTour(
            @AuthenticationPrincipal UUID currentUserId,
            @RequestBody RequestTourDto dto) {
        SiteVisit visit = siteVisitService.createVisit(
                currentUserId, dto.getListingId(), dto.getRequestedTime(), dto.getNotes());
        return ResponseEntity.ok(visit);
    }

    /** GET /api/tours/visitor — Get visitor's tours */
    @GetMapping("/visitor")
    public ResponseEntity<List<TourResponse>> getVisitorTours(@AuthenticationPrincipal UUID currentUserId) {
        List<SiteVisit> visits = siteVisitService.getVisitorTours(currentUserId);
        return ResponseEntity.ok(enrichTours(visits));
    }

    /** GET /api/tours/owner — Get owner's tours */
    @GetMapping("/owner")
    public ResponseEntity<List<TourResponse>> getOwnerTours(@AuthenticationPrincipal UUID currentUserId) {
        List<SiteVisit> visits = siteVisitService.getOwnerTours(currentUserId);
        return ResponseEntity.ok(enrichTours(visits));
    }

    /** PATCH /api/tours/{id}/status — Update tour status (owner or participant) */
    @PatchMapping("/{id}/status")
    public ResponseEntity<SiteVisit> updateStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID currentUserId,
            @RequestParam String status) {
        TourStatus newStatus = TourStatus.fromValue(status);
        SiteVisit visit = siteVisitService.updateStatus(currentUserId, id, newStatus);
        return ResponseEntity.ok(visit);
    }

    private List<TourResponse> enrichTours(List<SiteVisit> visits) {
        return visits.stream().map(v -> {
            Listing listing = listingRepository.findById(v.getListingId()).orElse(null);
            TourResponse r = new TourResponse();
            r.setId(v.getId());
            r.setListingId(v.getListingId());
            r.setVisitorId(v.getVisitorId());
            r.setOwnerId(v.getOwnerId());
            r.setRequestedTime(v.getRequestedTime());
            r.setStatus(v.getStatus());
            r.setNotes(v.getNotes());
            r.setCreatedAt(v.getCreatedAt());
            r.setUpdatedAt(v.getUpdatedAt());
            if (listing != null) {
                r.setListingTitle(listing.getTitle());
                r.setListingAddress(listing.getAddressText());
                r.setListingRent(listing.getRentAmount());
            }
            return r;
        }).collect(Collectors.toList());
    }

    @Getter @Setter
    public static class RequestTourDto {
        private UUID listingId;
        private OffsetDateTime requestedTime;
        private String notes;
    }

    @Getter @Setter
    public static class TourResponse {
        private UUID id;
        private UUID listingId;
        private UUID visitorId;
        private UUID ownerId;
        private OffsetDateTime requestedTime;
        private TourStatus status;
        private String notes;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private String listingTitle;
        private String listingAddress;
        private BigDecimal listingRent;
    }
}
