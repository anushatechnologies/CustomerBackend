package com.example.project.customer.dto.settlement;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RefundSettlementSnapshot(
        int totalQuantity,
        int refundQuantity,
        BigDecimal clawbackSellerPayable,
        BigDecimal reversalHinchKeeps
) {}
