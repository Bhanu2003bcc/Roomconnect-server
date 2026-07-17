package com.roomconnect.modules.listings.dto;

import com.roomconnect.modules.listings.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CreateListingRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Rent amount is required")
    @Positive(message = "Rent must be positive")
    private BigDecimal rentAmount;

    private BigDecimal depositAmount;

    private BathroomType bathroomType;

    private String furnishing;

    private GenderPreference genderPreference = GenderPreference.any;

    private boolean foodIncluded = false;

    private FoodType foodType;

    private LocalTime curfewTime;

    private AcType ac;

    private boolean wifi = false;
    private boolean parking = false;
    private boolean laundry = false;

    @NotBlank(message = "Address is required")
    private String addressText;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private ListingStatus status = ListingStatus.AVAILABLE;
    private LocalDate availableFromDate;
    private Integer cityId = 1;
}
