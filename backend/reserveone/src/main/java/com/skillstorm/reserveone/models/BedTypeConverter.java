package com.skillstorm.reserveone.models;

import java.util.Locale;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class BedTypeConverter implements AttributeConverter<RoomType.BedType, String> {

    @Override
    public String convertToDatabaseColumn(RoomType.BedType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RoomType.BedType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        String normalized = dbData.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }

        // Accept common DB variants like "queen" / "Queen" by normalizing case.
        return RoomType.BedType.valueOf(normalized);
    }
}
