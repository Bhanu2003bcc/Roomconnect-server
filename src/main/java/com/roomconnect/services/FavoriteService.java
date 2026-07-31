package com.roomconnect.services;

import com.roomconnect.models.Favorite;
import com.roomconnect.repositories.FavoriteRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    /** Toggle: adds if absent, removes if present. Returns true = now favorited. */
    @Transactional
    public boolean toggle(UUID visitorId, UUID listingId) {
        if (favoriteRepository.existsByVisitorIdAndListingId(visitorId, listingId)) {
            favoriteRepository.deleteByVisitorIdAndListingId(visitorId, listingId);
            return false;
        }
        favoriteRepository.save(Favorite.builder()
                .visitorId(visitorId)
                .listingId(listingId)
                .build());
        return true;
    }

    @Transactional(readOnly = true)
    public Page<Favorite> getFavorites(UUID visitorId, int page, int size) {
        return favoriteRepository.findByVisitorId(
                visitorId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(UUID visitorId, UUID listingId) {
        return favoriteRepository.existsByVisitorIdAndListingId(visitorId, listingId);
    }
}
