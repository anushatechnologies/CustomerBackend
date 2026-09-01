package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkPriceAdjustmentResponse {
    private Integer modifiedCount;
    private String adjustmentType;
    private Number value;
    private String applyTo;
    private Map<String, Object> summary;
    private List<Map<String, Object>> modifiedProducts;
}
