package com.example.project.customer.entity.converter;

import com.example.project.customer.entity.VendorInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter
public class VendorInfoConverter implements AttributeConverter<VendorInfo, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(VendorInfo attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Failed to convert VendorInfo to JSON string", e);
            return null;
        }
    }

    @Override
    public VendorInfo convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, VendorInfo.class);
        } catch (Exception e) {
            log.error("Failed to convert JSON string to VendorInfo: {}", dbData, e);
            return null;
        }
    }
}
