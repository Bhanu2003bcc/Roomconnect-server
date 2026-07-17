package com.roomconnect.modules.listings.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Builder.Default
    @Column(name = "city_id", nullable = false)
    private Integer cityId = 1;

    @Convert(converter = CategoryConverter.class)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rent_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "bathroom_type", length = 20)
    private BathroomType bathroomType;

    private String furnishing;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference", nullable = false, length = 20)
    private GenderPreference genderPreference = GenderPreference.any;

    @Builder.Default
    @Column(name = "food_included", nullable = false)
    private boolean foodIncluded = false;

    @Convert(converter = FoodTypeConverter.class)
    @Column(name = "food_type", length = 20)
    private FoodType foodType;

    @Column(name = "curfew_time")
    private LocalTime curfewTime;

    @Convert(converter = AcTypeConverter.class)
    @Column(length = 10)
    private AcType ac;

    @Builder.Default
    @Column(nullable = false)
    private boolean wifi = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean parking = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean laundry = false;

    @Column(name = "address_text", nullable = false, columnDefinition = "TEXT")
    private String addressText;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;
    // Note: 'geo' is a GENERATED ALWAYS column in Postgres (PostGIS geography).
    // It is NOT mapped here to avoid Hibernate schema validation conflicts.
    // All geo-distance queries use native SQL with ST_DWithin(geo, ...) directly.

    @Builder.Default
    @Convert(converter = ListingStatusConverter.class)
    @Column(nullable = false, length = 30)
    private ListingStatus status = ListingStatus.AVAILABLE;

    @Column(name = "available_from_date")
    private LocalDate availableFromDate;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
