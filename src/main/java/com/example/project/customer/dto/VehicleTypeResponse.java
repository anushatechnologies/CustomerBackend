package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleTypeResponse {

    private Integer id;
    private String code;
    private String name;
    private String description;
    private BigDecimal maxWeightKg;
    private String sizeDimensions;
    private BigDecimal baseFare;
    private Double baseDistanceKm;
    private BigDecimal perKmRate;
    private BigDecimal minimumFare;
    private Boolean active;
    private Integer sortOrder;
    private String imageUrl;
    private String fareSummary;
}
