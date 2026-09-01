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
public class SellerProductRequest {
    private String title;
    private String sku;
    private String description;
    private Integer categoryId;
    private Integer subcategoryId;
    private Integer brandId;
    private Long price;
    private Long sellingPrice;
    private Long mrp;
    private String unit;
    private Integer moq;
    private Integer stockQty;
    private Boolean is24HourDelivery;
    private List<String> images;
    private List<Map<String, Object>> bulkPricingTiers;
    private Map<String, String> specifications;
}
