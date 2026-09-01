package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkPriceAdjustmentRequest {
    private Integer categoryId;
    private Integer brandId;
    private String adjustmentType; // percentage_increase, percentage_decrease, fixed_increase, fixed_decrease
    private Number value;
    private String applyTo; // selling_price, mrp, both
}
