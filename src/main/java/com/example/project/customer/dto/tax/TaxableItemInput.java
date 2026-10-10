package com.example.project.customer.dto.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxableItemInput {
    private Integer productId;
    private String title;
    private String hsnCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal gstRate;
    private BigDecimal lineTotal;
}
