package com.roomconnect.modules.listings.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FoodTypeConverter implements AttributeConverter<FoodType, String> {

    @Override
    public String convertToDatabaseColumn(FoodType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public FoodType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return FoodType.fromValue(dbData);
    }
}
