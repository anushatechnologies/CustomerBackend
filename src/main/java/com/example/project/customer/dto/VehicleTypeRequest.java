package com.example.project.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTypeRequest {

    @NotBlank(message = "Vehicle code is required (e.g. TWO_WHEELER, THREE_WHEELER)")
    private String code;

    @NotBlank(message = "Vehicle name is required")
    private String name;

    private String description;

    @NotNull(message = "Maximum capacity weight (kg) is required")
    @DecimalMin(value = "0.01", message = "Max weight must be greater than zero")
    private BigDecimal maxWeightKg;

    private String sizeDimensions;

    @NotNull(message = "Base fare is required")
    @DecimalMin(value = "0.00", message = "Base fare must be >= 0")
    private BigDecimal baseFare;

    @NotNull(message = "Base distance in km is required")
    @DecimalMin(value = "0.00", message = "Base distance must be >= 0")
    private Double baseDistanceKm;

    @NotNull(message = "Per km rate is required")
    @DecimalMin(value = "0.00", message = "Per km rate must be >= 0")
    private BigDecimal perKmRate;

    @NotNull(message = "Minimum fare is required")
    @DecimalMin(value = "0.00", message = "Minimum fare must be >= 0")
    private BigDecimal minimumFare;

    private Boolean active;

    private Integer sortOrder;

    private String imageUrl;
}
