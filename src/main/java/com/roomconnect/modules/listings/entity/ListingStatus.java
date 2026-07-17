package com.roomconnect.modules.listings.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ListingStatus {
    AVAILABLE("available"),
    OCCUPIED("occupied"),
    AVAILABLE_FROM("available_from");

    private final String value;

    ListingStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ListingStatus fromValue(String value) {
        for (ListingStatus s : ListingStatus.values()) {
            if (s.value.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown listing status: " + value);
    }
}
