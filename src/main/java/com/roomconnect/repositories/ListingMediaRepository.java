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

    /**
     * Batch-loads cover media for a set of listing IDs in a single
     * WHERE listing_id IN (...) query — eliminates the N+1 pattern in search results.
     */
    List<ListingMedia> findByListingIdInAndProcessingStatusOrderBySortOrderAsc(List<UUID> listingIds, String processingStatus);

    int countByListingId(UUID listingId);
}
