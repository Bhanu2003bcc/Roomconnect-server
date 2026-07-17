package com.roomconnect.modules.listings.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ListingStatusConverter implements AttributeConverter<ListingStatus, String> {

    @Override
    public String convertToDatabaseColumn(ListingStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public ListingStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return ListingStatus.fromValue(dbData);
    }
}
