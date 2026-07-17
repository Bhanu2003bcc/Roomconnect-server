package com.roomconnect.modules.listings.service;

import com.roomconnect.modules.listings.dto.CreateListingRequest;
import com.roomconnect.modules.listings.entity.*;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.shared.exception.ForbiddenException;
import com.roomconnect.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;

    @Transactional
    public Listing createListing(UUID ownerId, CreateListingRequest req) {
        Listing listing = Listing.builder()
                .ownerId(ownerId)
                .cityId(req.getCityId() != null ? req.getCityId() : 1)
                .category(req.getCategory())
                .title(req.getTitle())
                .description(req.getDescription())
                .rentAmount(req.getRentAmount())
                .depositAmount(req.getDepositAmount())
                .bathroomType(req.getBathroomType())
                .furnishing(req.getFurnishing())
                .genderPreference(req.getGenderPreference() != null ? req.getGenderPreference() : GenderPreference.any)
                .foodIncluded(req.isFoodIncluded())
                .foodType(req.getFoodType())
                .curfewTime(req.getCurfewTime())
                .ac(req.getAc())
                .wifi(req.isWifi())
                .parking(req.isParking())
                .laundry(req.isLaundry())
                .addressText(req.getAddressText())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .status(req.getStatus() != null ? req.getStatus() : ListingStatus.AVAILABLE)
                .availableFromDate(req.getAvailableFromDate())
                .build();
        return listingRepository.save(listing);
    }

    @Transactional
    public Listing updateListing(UUID listingId, UUID requesterId, CreateListingRequest req) {
        Listing listing = getListingOrThrow(listingId);
        assertOwnership(listing, requesterId);

        listing.setCategory(req.getCategory());
        listing.setTitle(req.getTitle());
        listing.setDescription(req.getDescription());
        listing.setRentAmount(req.getRentAmount());
        listing.setDepositAmount(req.getDepositAmount());
        listing.setBathroomType(req.getBathroomType());
        listing.setFurnishing(req.getFurnishing());
        listing.setGenderPreference(req.getGenderPreference() != null ? req.getGenderPreference() : GenderPreference.any);
        listing.setFoodIncluded(req.isFoodIncluded());
        listing.setFoodType(req.getFoodType());
        listing.setCurfewTime(req.getCurfewTime());
        listing.setAc(req.getAc());
        listing.setWifi(req.isWifi());
        listing.setParking(req.isParking());
        listing.setLaundry(req.isLaundry());
        listing.setAddressText(req.getAddressText());
        listing.setLatitude(req.getLatitude());
        listing.setLongitude(req.getLongitude());
        listing.setAvailableFromDate(req.getAvailableFromDate());
        listing.setUpdatedAt(OffsetDateTime.now());

        return listingRepository.save(listing);
    }

    @Transactional
    public void updateStatus(UUID listingId, UUID requesterId, ListingStatus newStatus) {
        Listing listing = getListingOrThrow(listingId);
        assertOwnership(listing, requesterId);
        listing.setStatus(newStatus);
        listing.setUpdatedAt(OffsetDateTime.now());
        listingRepository.save(listing);
    }

    @Transactional
    public void deleteListing(UUID listingId, UUID requesterId) {
        Listing listing = getListingOrThrow(listingId);
        assertOwnership(listing, requesterId);
        listingRepository.delete(listing);
    }

    @Transactional(readOnly = true)
    public Listing getById(UUID listingId) {
        return getListingOrThrow(listingId);
    }

    @Transactional(readOnly = true)
    public Page<Listing> getOwnerListings(UUID ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return listingRepository.findByOwnerId(ownerId, pageable);
    }

    /** Scheduled job: flip listings where available_from_date <= today to AVAILABLE */
    @Scheduled(cron = "0 0 6 * * *") // 06:00 IST daily
    @Transactional
    public void activateScheduledListings() {
        List<Listing> toActivate = listingRepository
                .findByStatusAndAvailableFromDateLessThanEqual(ListingStatus.AVAILABLE_FROM, LocalDate.now());
        if (toActivate.isEmpty()) return;
        toActivate.forEach(l -> {
            l.setStatus(ListingStatus.AVAILABLE);
            l.setUpdatedAt(OffsetDateTime.now());
        });
        listingRepository.saveAll(toActivate);
        log.info("Activated {} scheduled listings", toActivate.size());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Listing getListingOrThrow(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + id));
    }

    private void assertOwnership(Listing listing, UUID requesterId) {
        if (!listing.getOwnerId().equals(requesterId)) {
            throw new ForbiddenException("You do not own this listing");
        }
    }
}
