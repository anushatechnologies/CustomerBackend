package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProductResponse {
    private String id;
    private String sellerId;
    private String title;
    private String sku;
    private String description;
    private Integer brandId;
    private String brandName;
    private Integer categoryId;
    private String categoryName;
    private Integer subcategoryId;
    private String subcategoryName;
    private Long price;
    private Long sellingPrice;
    private Long mrp;
    private String unit;
    private Integer moq;
    private Integer stockQty;
    private Boolean is24HourDelivery;
    private String status;
    private List<String> images;
    private List<Map<String, Object>> bulkPricingTiers;
    private Map<String, String> specifications;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
