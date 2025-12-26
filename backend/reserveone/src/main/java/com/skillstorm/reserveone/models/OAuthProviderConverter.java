package com.skillstorm.reserveone.models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OAuthProviderConverter implements AttributeConverter<OAuthProvider, String> {

    @Override
    public String convertToDatabaseColumn(OAuthProvider attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public OAuthProvider convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OAuthProvider.fromDbValue(dbData);
    }
}
