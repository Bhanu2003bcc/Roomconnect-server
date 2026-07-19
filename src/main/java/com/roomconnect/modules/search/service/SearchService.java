package com.roomconnect.modules.search.service;

import com.roomconnect.modules.listings.entity.*;
import com.roomconnect.modules.listings.repository.ListingRepository;
import com.roomconnect.modules.media.service.MediaService;
import com.roomconnect.modules.search.dto.SearchRequest;
import com.roomconnect.modules.listings.dto.ListingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final JdbcTemplate jdbcTemplate;
    private final MediaService mediaService;

    /**
     * Full-featured geo+filter search using dynamic native SQL.
     * Results are sorted by distance from the search centre.
     */
    @Transactional(readOnly = true)
    public SearchResult search(SearchRequest req) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT l.* FROM listings l
                WHERE l.status = 'available'
                  AND ST_DWithin(
                        l.geo,
                        ST_SetSRID(ST_MakePoint(?, ?)::geography, 4326),
                        ?
                      )
                """);
        params.add(req.getLng());
        params.add(req.getLat());
        params.add(req.getRadiusMeters());

        if (req.getCategory() != null) {
            sql.append(" AND l.category = ?");
            params.add(req.getCategory().name());
        }
        if (req.getGenderPreference() != null) {
            sql.append(" AND l.gender_preference IN ('any', ?)");
            params.add(req.getGenderPreference().name());
        }
        if (req.getMinRent() != null) {
            sql.append(" AND l.rent_amount >= ?");
            params.add(req.getMinRent());
        }
        if (req.getMaxRent() != null) {
            sql.append(" AND l.rent_amount <= ?");
            params.add(req.getMaxRent());
        }
        if (Boolean.TRUE.equals(req.getWifi()))         { sql.append(" AND l.wifi = true"); }
        if (Boolean.TRUE.equals(req.getParking()))      { sql.append(" AND l.parking = true"); }
        if (Boolean.TRUE.equals(req.getLaundry()))      { sql.append(" AND l.laundry = true"); }
        if (Boolean.TRUE.equals(req.getFoodIncluded())) { sql.append(" AND l.food_included = true"); }
        if (req.getAc() != null) {
            sql.append(" AND l.ac = ?");
            params.add(req.getAc().name());
        }
        if (req.getBathroomType() != null) {
            sql.append(" AND l.bathroom_type = ?");
            params.add(req.getBathroomType().name());
        }

        // Count query
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") sub";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        // Paginated results ordered by distance
        sql.append("""
                 ORDER BY ST_Distance(l.geo, ST_SetSRID(ST_MakePoint(?, ?)::geography, 4326)) ASC
                 LIMIT ? OFFSET ?
                """);
        params.add(req.getLng());
        params.add(req.getLat());
        params.add(req.getSize());
        params.add((long) req.getPage() * req.getSize());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        List<ListingResponse> results = rows.stream()
                .map(this::mapRow)
                .collect(Collectors.toList());

        return new SearchResult(results, total != null ? total : 0L, req.getPage(), req.getSize());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ListingResponse mapRow(Map<String, Object> row) {
        ListingResponse r = new ListingResponse();
        r.setId((UUID) row.get("id"));
        r.setOwnerId((UUID) row.get("owner_id"));
        r.setCityId((Integer) row.get("city_id"));
        r.setTitle((String) row.get("title"));
        r.setDescription((String) row.get("description"));
        r.setAddressText((String) row.get("address_text"));
        r.setLatitude((Double) row.get("latitude"));
        r.setLongitude((Double) row.get("longitude"));
        Object rent = row.get("rent_amount");
        if (rent instanceof BigDecimal bd) r.setRentAmount(bd);
        Object deposit = row.get("deposit_amount");
        if (deposit instanceof BigDecimal bd) r.setDepositAmount(bd);
        r.setWifi(Boolean.TRUE.equals(row.get("wifi")));
        r.setParking(Boolean.TRUE.equals(row.get("parking")));
        r.setLaundry(Boolean.TRUE.equals(row.get("laundry")));
        r.setFoodIncluded(Boolean.TRUE.equals(row.get("food_included")));

        String cat = (String) row.get("category");
        if (cat != null) r.setCategory(Category.fromValue(cat));
        String gp  = (String) row.get("gender_preference");
        if (gp != null)  r.setGenderPreference(GenderPreference.valueOf(gp));
        String stat = (String) row.get("status");
        if (stat != null) r.setStatus(ListingStatus.fromValue(stat));

        Object created = row.get("created_at");
        if (created instanceof java.sql.Timestamp ts) {
            r.setCreatedAt(ts.toInstant().atOffset(java.time.ZoneOffset.UTC));
        } else if (created instanceof OffsetDateTime odt) {
            r.setCreatedAt(odt);
        }
        Object updated = row.get("updated_at");
        if (updated instanceof java.sql.Timestamp ts) {
            r.setUpdatedAt(ts.toInstant().atOffset(java.time.ZoneOffset.UTC));
        } else if (updated instanceof OffsetDateTime odt) {
            r.setUpdatedAt(odt);
        }

        // Attach media (cover photo only for list view)
        List<ListingResponse.MediaItem> media = mediaService.getListingMedia(r.getId())
                .stream().limit(1)
                .map(m -> {
                    ListingResponse.MediaItem item = new ListingResponse.MediaItem();
                    item.setId(m.getId());
                    item.setUrl(mediaService.getPublicUrl(m.getFileKey()));
                    item.setThumbnailUrl(mediaService.getPublicUrl(m.getThumbnailKey()));
                    item.setSortOrder(m.getSortOrder());
                    return item;
                }).toList();
        r.setMedia(media);

        return r;
    }

    public record SearchResult(List<ListingResponse> items, long total, int page, int size) {}
}
