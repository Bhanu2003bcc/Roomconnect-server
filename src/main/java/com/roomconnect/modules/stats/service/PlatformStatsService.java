package com.roomconnect.modules.stats.service;

import com.roomconnect.modules.listings.entity.ListingStatus;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.modules.stats.dto.PlatformStatsResponse;
import com.roomconnect.modules.users.repository.OwnerProfileRepository;
import com.roomconnect.modules.users.repository.VisitorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformStatsService {

    private final ListingRepository listingRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final VisitorProfileRepository visitorProfileRepository;

    /** Simple in-memory cache: refresh at most once every 30 seconds. */
    private final AtomicReference<CachedStats> cache = new AtomicReference<>();

    private static final long CACHE_TTL_MS = 30_000L;
    private static final long BASELINE_ROOMS   = 500L;
    private static final long BASELINE_OWNERS  = 200L;
    private static final long BASELINE_TENANTS = 1200L;
    private static final int  AVG_DAYS_TO_MOVE = 3;

    public PlatformStatsResponse getStats() {
        CachedStats cached = cache.get();
        if (cached != null && Instant.now().toEpochMilli() - cached.fetchedAt() < CACHE_TTL_MS) {
            return cached.stats();
        }

        long dbRooms   = listingRepository.countByStatus(ListingStatus.AVAILABLE);
        long dbOwners  = ownerProfileRepository.count();
        long dbTenants = visitorProfileRepository.count();

        long rooms   = BASELINE_ROOMS + dbRooms;
        long owners  = BASELINE_OWNERS + dbOwners;
        long tenants = BASELINE_TENANTS + dbTenants;

        PlatformStatsResponse stats = new PlatformStatsResponse(rooms, owners, tenants, AVG_DAYS_TO_MOVE);
        cache.set(new CachedStats(stats, Instant.now().toEpochMilli()));
        log.debug("Platform stats refreshed: rooms={}, owners={}, tenants={}", rooms, owners, tenants);
        return stats;
    }

    private record CachedStats(PlatformStatsResponse stats, long fetchedAt) {}
}
