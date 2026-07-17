package com.roomconnect.modules.listings.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AcType {
    AC("ac"),
    NON_AC("non-ac");

    private final String value;

    AcType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AcType fromValue(String value) {
        for (AcType a : AcType.values()) {
            if (a.value.equalsIgnoreCase(value)) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown AC type: " + value);
    }
}
