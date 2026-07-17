package com.roomconnect.modules.tours.service;

import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.modules.notifications.service.NotificationService;
import com.roomconnect.modules.tours.entity.SiteVisit;
import com.roomconnect.modules.tours.entity.SiteVisit.TourStatus;
import com.roomconnect.modules.tours.repository.SiteVisitRepository;
import com.roomconnect.shared.exception.ForbiddenException;
import com.roomconnect.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteVisitService {

    private final SiteVisitRepository siteVisitRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    @Transactional
    public SiteVisit createVisit(UUID visitorId, UUID listingId, OffsetDateTime requestedTime, String notes) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));

        SiteVisit visit = SiteVisit.builder()
                .listingId(listingId)
                .visitorId(visitorId)
                .ownerId(listing.getOwnerId())
                .requestedTime(requestedTime)
                .notes(notes)
                .status(TourStatus.REQUESTED)
                .build();

        SiteVisit saved = siteVisitRepository.save(visit);
        log.info("Site visit requested for listing {} by visitor {}", listingId, visitorId);

        // Notify owner
        notificationService.sendSms(
                "OWNER_PHONE", // In v1 we would lookup the owner profile phone or log
                "New site visit requested for your listing: " + listing.getTitle() + " at " + requestedTime
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<SiteVisit> getVisitorTours(UUID visitorId) {
        return siteVisitRepository.findByVisitorIdOrderByRequestedTimeDesc(visitorId);
    }

    @Transactional(readOnly = true)
    public List<SiteVisit> getOwnerTours(UUID ownerId) {
        return siteVisitRepository.findByOwnerIdOrderByRequestedTimeDesc(ownerId);
    }

    @Transactional
    public SiteVisit updateStatus(UUID requesterId, UUID tourId, TourStatus newStatus) {
        SiteVisit visit = siteVisitRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Site visit not found: " + tourId));

        if (newStatus == TourStatus.CONFIRMED || newStatus == TourStatus.DECLINED || newStatus == TourStatus.COMPLETED) {
            if (!visit.getOwnerId().equals(requesterId)) {
                throw new ForbiddenException("Only the owner can update the visit request status to " + newStatus);
            }
        } else if (newStatus == TourStatus.CANCELLED) {
            if (!visit.getVisitorId().equals(requesterId) && !visit.getOwnerId().equals(requesterId)) {
                throw new ForbiddenException("Unauthorized to cancel this visit");
            }
        }

        visit.setStatus(newStatus);
        visit.setUpdatedAt(OffsetDateTime.now());
        SiteVisit updated = siteVisitRepository.save(visit);
        log.info("Site visit {} updated to status {} by user {}", tourId, newStatus, requesterId);

        // Notify other party
        String msg = "Your site visit request has been " + newStatus.getValue();
        if (visit.getVisitorId().equals(requesterId)) {
            // Visitor cancelled, notify owner
            notificationService.sendSms("OWNER_PHONE", "Visitor cancelled site visit request for listing " + visit.getListingId());
        } else {
            // Owner action, notify visitor
            notificationService.sendSms("VISITOR_PHONE", msg);
        }

        return updated;
    }
}
