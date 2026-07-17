package com.roomconnect.modules.listings.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AcTypeConverter implements AttributeConverter<AcType, String> {

    @Override
    public String convertToDatabaseColumn(AcType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public AcType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return AcType.fromValue(dbData);
    }
}
