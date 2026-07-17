package com.roomconnect.modules.listings.repository;

import com.roomconnect.modules.listings.entity.Listing;
import com.roomconnect.modules.listings.entity.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {

    Page<Listing> findByOwnerId(UUID ownerId, Pageable pageable);

    List<Listing> findByStatusAndAvailableFromDateLessThanEqual(
            ListingStatus status, LocalDate date);

    /**
     * Geo-radius search using PostGIS ST_DWithin on the geography column.
     * Returns listings within radiusMeters of the given lat/lng centre.
     */
    @Query(value = """
            SELECT * FROM listings
            WHERE status = 'AVAILABLE'
              AND ST_DWithin(
                    geo,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            ORDER BY ST_Distance(geo, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) ASC
            LIMIT :limit OFFSET :offset
            """,
            nativeQuery = true)
    List<Listing> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = """
            SELECT COUNT(*) FROM listings
            WHERE status = 'AVAILABLE'
              AND ST_DWithin(
                    geo,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            """,
            nativeQuery = true)
    long countNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters);
}
