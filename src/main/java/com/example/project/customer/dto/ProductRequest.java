package com.example.project.customer.dto;

import com.example.project.customer.entity.BulkPricingTier;
import com.example.project.customer.entity.VendorInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
public class ProductRequest {

    private Integer brandId;

    private Integer subcategoryId;

    private Integer categoryId;

    @NotBlank(message = "Product title is required")
    private String title;

    private String slug;

    private String sku;

    private String brand;

    private String description;

    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private BigDecimal mrp;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    private Integer stockQty;

    @NotBlank(message = "Unit of measurement is required")
    private String unit;

    @Builder.Default
    private Integer moq = 1;

    private String imageUrl;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Builder.Default
    @JsonProperty("active")
    private Boolean active = true;

    @Builder.Default
    @JsonProperty("is24HourDelivery")
    private Boolean is24HourDelivery = false;

    private Double rating;

    private Integer reviewCount;

    private BigDecimal gstRate;

    private String hsnCode;

    @Builder.Default
    private Map<String, String> specifications = new LinkedHashMap<>();

    @Builder.Default
    private List<BulkPricingTier> bulkPricingTiers = new ArrayList<>();

    private VendorInfo vendor;
}