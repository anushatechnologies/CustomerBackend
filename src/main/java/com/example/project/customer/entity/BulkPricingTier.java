package com.example.project.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPricingTier {
    private Integer tierId;
    private Integer minQty;
    private Integer maxQty;
    private BigDecimal price;
    private Double discountPercentage;
}
