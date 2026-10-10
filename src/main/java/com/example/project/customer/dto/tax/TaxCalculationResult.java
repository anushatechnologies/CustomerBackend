package com.example.project.customer.dto.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCalculationResult {
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal totalGst;
    private BigDecimal freightCharge;
    private BigDecimal craneUnloadingCharge;
    private BigDecimal grandTotal;
    private boolean intraState;

    @Builder.Default
    private List<TaxableItemResult> itemResults = new ArrayList<>();
}
