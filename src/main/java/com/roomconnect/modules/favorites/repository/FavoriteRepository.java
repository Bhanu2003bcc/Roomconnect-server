package com.roomconnect.modules.favorites.repository;

import com.roomconnect.modules.favorites.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.FavoriteId> {

    Optional<Favorite> findByVisitorIdAndListingId(UUID visitorId, UUID listingId);

    boolean existsByVisitorIdAndListingId(UUID visitorId, UUID listingId);

    Page<Favorite> findByVisitorId(UUID visitorId, Pageable pageable);

    void deleteByVisitorIdAndListingId(UUID visitorId, UUID listingId);
}
