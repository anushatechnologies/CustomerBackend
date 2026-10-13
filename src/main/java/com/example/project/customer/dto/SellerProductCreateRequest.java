package com.example.project.customer.dto;

import com.example.project.customer.entity.BulkPricingTier;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerProductCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String sku;

    private String description;

    private Integer categoryId;

    private Integer subcategoryId;

    @NotNull(message = "Brand ID is required")
    private Integer brandId;

    private BigDecimal price;

    private BigDecimal sellingPrice;

    private BigDecimal mrp;

    @NotBlank(message = "Unit is required")
    private String unit;

    @Builder.Default
    private Integer moq = 1;

    @Builder.Default
    private Integer stockQty = 0;

    @JsonProperty("is24HourDelivery")
    @Builder.Default
    private Boolean is24HourDelivery = false;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Builder.Default
    private List<BulkPricingTier> bulkPricingTiers = new ArrayList<>();

    @Builder.Default
    private Map<String, String> specifications = new LinkedHashMap<>();

    public BigDecimal getEffectivePrice() {
        if (sellingPrice != null) {
            return sellingPrice;
        }
        return price != null ? price : BigDecimal.ZERO;
    }
}
