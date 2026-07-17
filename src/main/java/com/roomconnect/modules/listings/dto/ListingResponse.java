package com.roomconnect.modules.listings.dto;

import com.roomconnect.modules.listings.entity.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ListingResponse {
    private UUID id;
    private UUID ownerId;
    private Integer cityId;
    private Category category;
    private String title;
    private String description;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private BathroomType bathroomType;
    private String furnishing;
    private GenderPreference genderPreference;
    private boolean foodIncluded;
    private FoodType foodType;
    private LocalTime curfewTime;
    private AcType ac;
    private boolean wifi;
    private boolean parking;
    private boolean laundry;
    private String addressText;
    private Double latitude;
    private Double longitude;
    private ListingStatus status;
    private LocalDate availableFromDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<MediaItem> media;

    @Getter @Setter
    public static class MediaItem {
        private UUID id;
        private String url;
        private String thumbnailUrl;
        private Integer sortOrder;
    }
}
