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
public class PurchaseOrderItemResponse {
    private Integer itemId;
    private Integer productId;
    private String productTitle;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private BigDecimal lineTotal;
}
