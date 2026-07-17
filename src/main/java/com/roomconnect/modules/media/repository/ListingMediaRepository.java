package com.roomconnect.modules.media.repository;

import com.roomconnect.modules.media.entity.ListingMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListingMediaRepository extends JpaRepository<ListingMedia, UUID> {
    List<ListingMedia> findByListingIdOrderBySortOrderAsc(UUID listingId);
    int countByListingId(UUID listingId);
}
