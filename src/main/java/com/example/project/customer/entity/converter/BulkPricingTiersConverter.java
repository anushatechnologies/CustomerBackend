package com.example.project.customer.entity.converter;

import com.example.project.customer.entity.BulkPricingTier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Converter
public class BulkPricingTiersConverter implements AttributeConverter<List<BulkPricingTier>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<BulkPricingTier> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Failed to convert bulk pricing tiers to JSON string", e);
            return "[]";
        }
    }

    @Override
    public List<BulkPricingTier> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<BulkPricingTier>>() {});
        } catch (Exception e) {
            log.error("Failed to convert JSON string to BulkPricingTier list: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}
