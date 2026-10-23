package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {
    private Integer cartItemId;
    private Integer productId;
    private String title;
    private String imageUrl;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal originalPrice;
    private String appliedTier;
    private BigDecimal gstRate;
    private BigDecimal lineTotal;
    private BigDecimal lineGst;
    private String hsnCode;
    private Integer availableQty;
    private Boolean inStock;
    private String reason;
    private Integer storeId;
    private String storeName;
    private String storeSlug;
}
