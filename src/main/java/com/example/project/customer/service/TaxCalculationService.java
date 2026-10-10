package com.example.project.customer.service;

import com.example.project.customer.dto.tax.TaxCalculationResult;
import com.example.project.customer.dto.tax.TaxableItemInput;

import java.math.BigDecimal;
import java.util.List;

public interface TaxCalculationService {

    /**
     * Determines whether a transaction is intra-state (CGST + SGST) or inter-state (IGST).
     *
     * @param originState The seller / store state of origin.
     * @param buyerState  The buyer's delivery destination state.
     * @return true if intra-state, false if inter-state.
     * @throws IllegalArgumentException if buyerState is null or blank.
     */
    boolean isIntraState(String originState, String buyerState);

    /**
     * Calculates line-item and order-level taxes for tax-exclusive line items,
     * allocating coupon discounts proportionally across items.
     *
     * @param items                The list of line items to calculate taxes for.
     * @param couponDiscount       The overall coupon discount applied to the order.
     * @param isIntraState         True if supply is within the same state (CGST + SGST), false for inter-state (IGST).
     * @param freightCharge        Optional freight / transportation charge.
     * @param craneUnloadingCharge Optional crane unloading charge.
     * @return The aggregated tax calculation result.
     */
    TaxCalculationResult calculateTaxes(List<TaxableItemInput> items,
                                        BigDecimal couponDiscount,
                                        boolean isIntraState,
                                        BigDecimal freightCharge,
                                        BigDecimal craneUnloadingCharge);

    /**
     * Calculates tax extraction from a tax-inclusive total contract amount (e.g. for RFQ quotations).
     *
     * @param totalAmount  The gross total amount (tax-inclusive).
     * @param gstRate      The applicable GST percentage rate.
     * @param isIntraState True if intra-state, false if inter-state.
     * @return The aggregated tax calculation result.
     */
    TaxCalculationResult calculateTaxInclusive(BigDecimal totalAmount,
                                               BigDecimal gstRate,
                                               boolean isIntraState);
}
