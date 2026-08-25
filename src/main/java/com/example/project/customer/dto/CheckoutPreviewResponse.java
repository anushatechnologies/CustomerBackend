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
public class CheckoutPreviewResponse {
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
}
