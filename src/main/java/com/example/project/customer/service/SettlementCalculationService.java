package com.example.project.customer.service;

import com.example.project.customer.dto.settlement.ItemSettlementSnapshot;
import com.example.project.customer.dto.settlement.RefundSettlementSnapshot;

import java.math.BigDecimal;

public interface SettlementCalculationService {

    ItemSettlementSnapshot calculateItemSettlement(
            boolean isHinchStore,
            BigDecimal preDiscountAmount,
            BigDecimal allocatedDiscount,
            Integer categoryId
    );

    RefundSettlementSnapshot calculateRefundSettlement(
            BigDecimal originalSellerPayable,
            BigDecimal originalHinchKeeps,
            int totalQuantity,
            int refundQuantity
    );
}
