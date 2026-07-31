package com.roomconnect.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.roomconnect.models.ListingMedia;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListingMediaRepository extends JpaRepository<ListingMedia, UUID> {
    List<ListingMedia> findByListingIdOrderBySortOrderAsc(UUID listingId);
    List<ListingMedia> findByListingIdAndProcessingStatusOrderBySortOrderAsc(UUID listingId, String processingStatus);
    int countByListingId(UUID listingId);
}
