package com.roomconnect.modules.listings.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
    PG("pg"),
    BHK1("1bhk"),
    BHK2("2bhk"),
    BHK3("3bhk"),
    INDEPENDENT_ROOM("independent_room");

    private final String value;

    Category(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Category fromValue(String value) {
        for (Category c : Category.values()) {
            if (c.value.equalsIgnoreCase(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown category: " + value);
    }
}
