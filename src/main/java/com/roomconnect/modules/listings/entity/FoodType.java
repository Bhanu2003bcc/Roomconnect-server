package com.roomconnect.modules.listings.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FoodType {
    VEG("veg"),
    NON_VEG("non-veg"),
    BOTH("both"),
    NONE("none");

    private final String value;

    FoodType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FoodType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (FoodType f : FoodType.values()) {
            if (f.value.equalsIgnoreCase(value)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown food type: " + value);
    }
}
