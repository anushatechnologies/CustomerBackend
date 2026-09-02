package com.example.project.customer.dto;

import com.example.project.customer.entity.BulkPricingTier;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerProductUpdateRequest {

    private String title;
    private String sku;
    private String description;
    private Integer brandId;
    private BigDecimal price;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private String unit;
    private Integer moq;
    private Integer stockQty;

    @JsonProperty("is24HourDelivery")
    private Boolean is24HourDelivery;

    private List<String> images;
    private List<BulkPricingTier> bulkPricingTiers;
    private Map<String, String> specifications;

    public BigDecimal getEffectivePrice() {
        if (sellingPrice != null) {
            return sellingPrice;
        }
        return price;
    }
}
