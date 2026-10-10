package com.example.project.customer.service;

import com.example.project.customer.dto.tax.TaxCalculationResult;
import com.example.project.customer.dto.tax.TaxableItemInput;
import com.example.project.customer.dto.tax.TaxableItemResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TaxCalculationServiceImpl implements TaxCalculationService {

    private static final String DEFAULT_ORIGIN_STATE = "Telangana";

    private static final Map<String, String> STATE_CODE_OR_ALIAS_MAP = Map.ofEntries(
            Map.entry("36", "TELANGANA"),
            Map.entry("TS", "TELANGANA"),
            Map.entry("TG", "TELANGANA"),
            Map.entry("TELANGANA", "TELANGANA"),
            Map.entry("29", "KARNATAKA"),
            Map.entry("KA", "KARNATAKA"),
            Map.entry("KARNATAKA", "KARNATAKA"),
            Map.entry("27", "MAHARASHTRA"),
            Map.entry("MH", "MAHARASHTRA"),
            Map.entry("MAHARASHTRA", "MAHARASHTRA"),
            Map.entry("33", "TAMIL NADU"),
            Map.entry("TN", "TAMIL NADU"),
            Map.entry("TAMIL NADU", "TAMIL NADU"),
            Map.entry("TAMILNADU", "TAMIL NADU"),
            Map.entry("37", "ANDHRA PRADESH"),
            Map.entry("AP", "ANDHRA PRADESH"),
            Map.entry("ANDHRA PRADESH", "ANDHRA PRADESH"),
            Map.entry("07", "DELHI"),
            Map.entry("DL", "DELHI"),
            Map.entry("DELHI", "DELHI")
    );

    @Override
    public boolean isIntraState(String originState, String buyerState) {
        if (buyerState == null || buyerState.trim().isBlank()) {
            throw new IllegalArgumentException("Delivery address state is required for GST determination.");
        }

        String effectiveOrigin = (originState != null && !originState.trim().isBlank())
                ? originState.trim()
                : DEFAULT_ORIGIN_STATE;

        String normOrigin = normalizeState(effectiveOrigin);
        String normBuyer = normalizeState(buyerState.trim());

        return normOrigin.equalsIgnoreCase(normBuyer);
    }

    @Override
    public TaxCalculationResult calculateTaxes(List<TaxableItemInput> items,
                                               BigDecimal couponDiscount,
                                               boolean isIntraState,
                                               BigDecimal freightCharge,
                                               BigDecimal craneUnloadingCharge) {
        if (items == null || items.isEmpty()) {
            BigDecimal freight = (freightCharge != null) ? freightCharge : BigDecimal.ZERO;
            BigDecimal crane = (craneUnloadingCharge != null) ? craneUnloadingCharge : BigDecimal.ZERO;
            return TaxCalculationResult.builder()
                    .subtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .discount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .taxableAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .cgst(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .sgst(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .igst(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalGst(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .freightCharge(freight.setScale(2, RoundingMode.HALF_UP))
                    .craneUnloadingCharge(crane.setScale(2, RoundingMode.HALF_UP))
                    .grandTotal(freight.add(crane).setScale(2, RoundingMode.HALF_UP))
                    .intraState(isIntraState)
                    .itemResults(List.of())
                    .build();
        }

        // 1. Validate each item and calculate line totals
        BigDecimal subtotal = BigDecimal.ZERO;
        List<BigDecimal> itemLineTotals = new ArrayList<>(items.size());

        for (TaxableItemInput item : items) {
            BigDecimal rate = item.getGstRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalStateException("Product '" + item.getTitle() + "' (ID: "
                        + item.getProductId() + ") has missing or invalid GST tax rate: " + rate
                        + ". Tax calculation cannot proceed.");
            }

            BigDecimal lineTotal = item.getLineTotal();
            if (lineTotal == null) {
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            }
            itemLineTotals.add(lineTotal);
            subtotal = subtotal.add(lineTotal);
        }

        // 2. Allocate coupon discount proportionally across items
        BigDecimal discount = (couponDiscount != null && couponDiscount.compareTo(BigDecimal.ZERO) > 0)
                ? couponDiscount
                : BigDecimal.ZERO;

        BigDecimal effectiveDiscount = discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
        List<BigDecimal> itemDiscounts = new ArrayList<>(items.size());

        if (effectiveDiscount.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal allocatedSum = BigDecimal.ZERO;
            for (int i = 0; i < items.size(); i++) {
                if (i == items.size() - 1) {
                    // Last item absorbs any rounding difference to ensure exact penny reconciliation
                    BigDecimal lastDiscount = effectiveDiscount.subtract(allocatedSum);
                    itemDiscounts.add(lastDiscount);
                } else {
                    BigDecimal lineTotal = itemLineTotals.get(i);
                    BigDecimal share = effectiveDiscount.multiply(lineTotal)
                            .divide(subtotal, 2, RoundingMode.HALF_UP);
                    itemDiscounts.add(share);
                    allocatedSum = allocatedSum.add(share);
                }
            }
        } else {
            for (int i = 0; i < items.size(); i++) {
                itemDiscounts.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
        }

        // 3. Compute item-level taxes on net taxable amounts
        List<TaxableItemResult> itemResults = new ArrayList<>(items.size());
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (int i = 0; i < items.size(); i++) {
            TaxableItemInput item = items.get(i);
            BigDecimal lineTotal = itemLineTotals.get(i);
            BigDecimal itemDiscount = itemDiscounts.get(i);
            BigDecimal lineTaxable = lineTotal.subtract(itemDiscount);
            if (lineTaxable.compareTo(BigDecimal.ZERO) < 0) {
                lineTaxable = BigDecimal.ZERO;
            }

            BigDecimal rate = item.getGstRate();
            BigDecimal lineCgst = BigDecimal.ZERO;
            BigDecimal lineSgst = BigDecimal.ZERO;
            BigDecimal lineIgst = BigDecimal.ZERO;

            if (isIntraState) {
                // Intra-State: Split into equal CGST and SGST
                BigDecimal halfRate = rate.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
                lineCgst = lineTaxable.multiply(halfRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                lineSgst = lineTaxable.multiply(halfRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                // Inter-State: Full IGST
                lineIgst = lineTaxable.multiply(rate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }

            BigDecimal lineGst = lineCgst.add(lineSgst).add(lineIgst);
            totalCgst = totalCgst.add(lineCgst);
            totalSgst = totalSgst.add(lineSgst);
            totalIgst = totalIgst.add(lineIgst);

            itemResults.add(TaxableItemResult.builder()
                    .productId(item.getProductId())
                    .title(item.getTitle())
                    .hsnCode(item.getHsnCode())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .originalLineTotal(lineTotal)
                    .allocatedDiscount(itemDiscount)
                    .lineTaxable(lineTaxable)
                    .gstRate(rate)
                    .lineCgst(lineCgst)
                    .lineSgst(lineSgst)
                    .lineIgst(lineIgst)
                    .lineGst(lineGst)
                    .build());
        }

        BigDecimal totalGst = totalCgst.add(totalSgst).add(totalIgst);
        BigDecimal taxableAmount = subtotal.subtract(effectiveDiscount);
        BigDecimal freight = (freightCharge != null) ? freightCharge : BigDecimal.ZERO;
        BigDecimal crane = (craneUnloadingCharge != null) ? craneUnloadingCharge : BigDecimal.ZERO;
        BigDecimal grandTotal = taxableAmount.add(totalGst).add(freight).add(crane);

        return TaxCalculationResult.builder()
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .discount(effectiveDiscount)
                .taxableAmount(taxableAmount.setScale(2, RoundingMode.HALF_UP))
                .cgst(totalCgst.setScale(2, RoundingMode.HALF_UP))
                .sgst(totalSgst.setScale(2, RoundingMode.HALF_UP))
                .igst(totalIgst.setScale(2, RoundingMode.HALF_UP))
                .totalGst(totalGst.setScale(2, RoundingMode.HALF_UP))
                .freightCharge(freight.setScale(2, RoundingMode.HALF_UP))
                .craneUnloadingCharge(crane.setScale(2, RoundingMode.HALF_UP))
                .grandTotal(grandTotal.setScale(2, RoundingMode.HALF_UP))
                .intraState(isIntraState)
                .itemResults(itemResults)
                .build();
    }

    @Override
    public TaxCalculationResult calculateTaxInclusive(BigDecimal totalAmount,
                                                       BigDecimal gstRate,
                                                       boolean isIntraState) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return TaxCalculationResult.builder()
                    .subtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .discount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .taxableAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .cgst(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .sgst(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .igst(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalGst(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .freightCharge(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .craneUnloadingCharge(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .grandTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .intraState(isIntraState)
                    .build();
        }

        BigDecimal rate = (gstRate != null) ? gstRate : BigDecimal.valueOf(18.0);
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalStateException("Invalid GST rate for tax-inclusive calculation: " + rate);
        }

        BigDecimal divisor = BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal taxable = totalAmount.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal totalGst = totalAmount.subtract(taxable);

        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;

        if (isIntraState) {
            cgst = totalGst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            sgst = totalGst.subtract(cgst);
        } else {
            igst = totalGst;
        }

        return TaxCalculationResult.builder()
                .subtotal(taxable)
                .discount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .taxableAmount(taxable)
                .cgst(cgst.setScale(2, RoundingMode.HALF_UP))
                .sgst(sgst.setScale(2, RoundingMode.HALF_UP))
                .igst(igst.setScale(2, RoundingMode.HALF_UP))
                .totalGst(totalGst.setScale(2, RoundingMode.HALF_UP))
                .freightCharge(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .craneUnloadingCharge(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .grandTotal(totalAmount.setScale(2, RoundingMode.HALF_UP))
                .intraState(isIntraState)
                .build();
    }

    private String normalizeState(String state) {
        if (state == null) return "";
        String cleaned = state.trim().toUpperCase().replaceAll("[^A-Z0-9 ]", "").trim();
        return STATE_CODE_OR_ALIAS_MAP.getOrDefault(cleaned, cleaned);
    }
}
