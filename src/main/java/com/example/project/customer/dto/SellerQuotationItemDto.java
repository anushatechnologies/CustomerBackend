package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerQuotationItemDto {

    private Object productId;
    private String name;
    private String productName;
    private Integer quantity;
    private String unit;
    private BigDecimal quotedRate;
    private BigDecimal gstRate;

    public String getName() {
        return name != null ? name : productName;
    }

    public String getProductName() {
        return productName != null ? productName : name;
    }
}
