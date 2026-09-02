package com.example.project.customer.dto;

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
public class BulkPriceAdjustmentRequest {

    private Integer categoryId;
    private Integer subcategoryId;
    private Integer brandId;
    private String brand;

    @NotNull(message = "adjustmentType is required (percentage_increase, percentage_decrease, fixed_increase, fixed_decrease)")
    private String adjustmentType;

    @NotNull(message = "value is required")
    private BigDecimal value;

    @Builder.Default
    private String applyTo = "both"; // selling_price, mrp, both
}
