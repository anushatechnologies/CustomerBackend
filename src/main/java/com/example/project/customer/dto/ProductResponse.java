package com.example.project.customer.dto;

import com.example.project.customer.entity.BulkPricingTier;
import com.example.project.customer.entity.VendorInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private Integer productId;

    private Integer subcategoryId;

    private Integer categoryId;

    private String title;

    private String slug;

    private String sku;

    private String brand;

    private String description;

    private String imageUrl;

    private List<String> images;

    private BigDecimal price;

    private BigDecimal mrp;

    private String unit;

    private Integer moq;

    private Integer stockQty;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("is24HourDelivery")
    private boolean is24HourDelivery;

    private Double rating;

    private Integer reviewCount;

    private BigDecimal gstRate;

    private String hsnCode;

    private Map<String, String> specifications;

    private List<BulkPricingTier> bulkPricingTiers;

    private VendorInfo vendor;

    // Admin approval information
    private String approvalStatus;

    private String status;

    private String rejectionReason;

    private LocalDateTime createdAt;
}