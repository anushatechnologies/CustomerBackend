package com.example.project.customer.service;

import com.example.project.customer.entity.BulkPricingTier;
import com.example.project.customer.entity.Estimation;
import com.example.project.customer.entity.EstimationItem;
import com.example.project.customer.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class EstimationCalculationServiceImpl implements EstimationCalculationService {

    @Override
    public void calculateItemPricing(EstimationItem item) {
        Product product = item.getMatchedProduct();
        if (product == null) {
            item.setUnitPrice(BigDecimal.ZERO);
            item.setAppliedTierDescription(null);
            item.setLineSubtotal(BigDecimal.ZERO);
            item.setLineTax(BigDecimal.ZERO);
            item.setLineTotal(BigDecimal.ZERO);
            item.setIsAvailable(false);
            return;
        }

        BigDecimal requestedQty = item.getRequestedQuantity() != null && item.getRequestedQuantity().compareTo(BigDecimal.ZERO) > 0
                ? item.getRequestedQuantity()
                : BigDecimal.ONE;

        int qtyInt = requestedQty.intValue();
        if (qtyInt < 1) {
            qtyInt = 1;
        }

        BigDecimal originalPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        BigDecimal unitPrice = originalPrice;
        String appliedTier = null;

        if (product.getBulkPricingTiers() != null && !product.getBulkPricingTiers().isEmpty()) {
            for (BulkPricingTier tier : product.getBulkPricingTiers()) {
                boolean minMatch = tier.getMinQty() == null || qtyInt >= tier.getMinQty();
                boolean maxMatch = tier.getMaxQty() == null || qtyInt <= tier.getMaxQty();
                if (minMatch && maxMatch && tier.getPrice() != null) {
                    unitPrice = tier.getPrice();
                    BigDecimal diff = originalPrice.subtract(unitPrice);
                    String maxQtyStr = tier.getMaxQty() != null ? String.valueOf(tier.getMaxQty()) : "+";
                    appliedTier = tier.getMinQty() + "-" + maxQtyStr + " " + product.getUnit() + " Tier (-₹" + diff.setScale(0, RoundingMode.HALF_UP) + "/" + product.getUnit() + ")";
                    break;
                }
            }
        }

        BigDecimal lineSubtotal = unitPrice.multiply(requestedQty).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstRate = product.getGstRate() != null ? product.getGstRate() : BigDecimal.valueOf(18.0);
        BigDecimal lineTax = lineSubtotal.multiply(gstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal lineTotal = lineSubtotal.add(lineTax).setScale(2, RoundingMode.HALF_UP);

        item.setUnitPrice(unitPrice);
        item.setAppliedTierDescription(appliedTier);
        item.setGstRate(gstRate);
        item.setLineSubtotal(lineSubtotal);
        item.setLineTax(lineTax);
        item.setLineTotal(lineTotal);
        item.setAvailableStock(product.getStockQty() != null ? product.getStockQty() : 0);
        item.setIsAvailable(product.isActive() && (product.getStockQty() != null && product.getStockQty() >= qtyInt));
    }

    @Override
    public void recalculateEstimationTotals(Estimation estimation) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        if (estimation.getItems() != null) {
            for (EstimationItem item : estimation.getItems()) {
                calculateItemPricing(item);
                if (item.getLineSubtotal() != null) {
                    subtotal = subtotal.add(item.getLineSubtotal());
                }
                if (item.getLineTax() != null) {
                    totalTax = totalTax.add(item.getLineTax());
                }
            }
        }

        BigDecimal discount = estimation.getDiscountAmount() != null ? estimation.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal grandTotal = subtotal.add(totalTax).subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        estimation.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        estimation.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        estimation.setDiscountAmount(discount.setScale(2, RoundingMode.HALF_UP));
        estimation.setGrandTotal(grandTotal);
    }
}
