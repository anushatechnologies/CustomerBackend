package com.example.project.customer.dto.settlement;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ItemSettlementSnapshot(
        boolean isHinchStore,
        BigDecimal preDiscountAmount,
        BigDecimal allocatedDiscount,
        BigDecimal effectiveSellingAmount,
        BigDecimal commissionRate,
        BigDecimal sellerPayable,
        BigDecimal hinchKeeps,
        BigDecimal tcsAmount,
        boolean marginDeficit
) {}
