package com.example.project.customer.entity.converter;

import com.example.project.customer.entity.VehicleType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Converter
public class VehicleTypeListConverter implements AttributeConverter<List<VehicleType>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<VehicleType> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("Failed to convert List<VehicleType> to JSON string", e);
            return "[]";
        }
    }

    @Override
    public List<VehicleType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<VehicleType>>() {});
        } catch (Exception e) {
            log.error("Failed to convert JSON string to List<VehicleType>: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}
