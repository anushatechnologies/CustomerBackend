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
public class PricingUpdateRequest {
    private Long sellingPrice;
    private Long mrp;
    private List<Map<String, Object>> bulkPricingTiers;
}
