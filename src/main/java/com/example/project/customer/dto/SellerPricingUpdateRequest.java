package com.example.project.customer.dto;

import com.example.project.customer.entity.BulkPricingTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPricingUpdateRequest {

    private BigDecimal price;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private List<BulkPricingTier> bulkPricingTiers;

    public BigDecimal getEffectiveSellingPrice() {
        if (sellingPrice != null) {
            return sellingPrice;
        }
        return price;
    }
}
