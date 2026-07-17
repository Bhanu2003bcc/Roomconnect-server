package com.roomconnect.modules.search.dto;

import com.roomconnect.modules.listings.entity.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SearchRequest {
    /** Centre-point of search (required) */
    private double lat;
    private double lng;

    /** Radius in km, default 5 km, max 20 km */
    private double radiusKm = 5.0;

    // ── filters ────────────────────────────────────────────────────────────
    private Category category;
    private GenderPreference genderPreference;
    private BigDecimal minRent;
    private BigDecimal maxRent;
    private Boolean wifi;
    private Boolean parking;
    private Boolean laundry;
    private Boolean foodIncluded;
    private AcType ac;
    private BathroomType bathroomType;

    // ── pagination ─────────────────────────────────────────────────────────
    private int page = 0;
    private int size = 20;

    public double getRadiusMeters() {
        return Math.min(radiusKm, 20.0) * 1000.0;
    }
}
