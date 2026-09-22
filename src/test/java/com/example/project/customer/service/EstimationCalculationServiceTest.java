package com.example.project.customer.service;

import com.example.project.customer.entity.BulkPricingTier;
import com.example.project.customer.entity.Estimation;
import com.example.project.customer.entity.EstimationItem;
import com.example.project.customer.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstimationCalculationServiceTest {

    private EstimationCalculationServiceImpl calculationService;
    private Product cementProduct;
    private Product steelProduct;

    @BeforeEach
    void setUp() {
        calculationService = new EstimationCalculationServiceImpl();

        BulkPricingTier tier1 = BulkPricingTier.builder()
                .tierId(1).minQty(50).maxQty(199).price(BigDecimal.valueOf(400.0)).build();
        BulkPricingTier tier2 = BulkPricingTier.builder()
                .tierId(2).minQty(200).maxQty(null).price(BigDecimal.valueOf(380.0)).build();

        cementProduct = Product.builder()
                .productId(1)
                .title("ABC OPC 53 Cement")
                .price(BigDecimal.valueOf(420.0))
                .unit("Bags")
                .gstRate(BigDecimal.valueOf(18.0))
                .stockQty(1000)
                .active(true)
                .bulkPricingTiers(List.of(tier1, tier2))
                .build();

        steelProduct = Product.builder()
                .productId(2)
                .title("TMT Steel Rebar 12mm")
                .price(BigDecimal.valueOf(70.0))
                .unit("kg")
                .gstRate(BigDecimal.valueOf(18.0))
                .stockQty(5000)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should apply base price when quantity is below bulk tier threshold")
    void testStandardPricing() {
        EstimationItem item = EstimationItem.builder()
                .matchedProduct(cementProduct)
                .requestedQuantity(BigDecimal.valueOf(20))
                .build();

        calculationService.calculateItemPricing(item);

        assertEquals(BigDecimal.valueOf(420.0), item.getUnitPrice());
        assertEquals(new BigDecimal("8400.00"), item.getLineSubtotal());
        assertEquals(new BigDecimal("1512.00"), item.getLineTax());
        assertEquals(new BigDecimal("9912.00"), item.getLineTotal());
    }

    @Test
    @DisplayName("Should apply volume bulk pricing tier when quantity qualifies")
    void testBulkVolumeTierPricing() {
        EstimationItem item = EstimationItem.builder()
                .matchedProduct(cementProduct)
                .requestedQuantity(BigDecimal.valueOf(100))
                .build();

        calculationService.calculateItemPricing(item);

        assertEquals(BigDecimal.valueOf(400.0), item.getUnitPrice());
        assertNotNull(item.getAppliedTierDescription());
        assertTrue(item.getAppliedTierDescription().contains("50-199 Bags Tier"));
        assertEquals(new BigDecimal("40000.00"), item.getLineSubtotal());
        assertEquals(new BigDecimal("7200.00"), item.getLineTax());
        assertEquals(new BigDecimal("47200.00"), item.getLineTotal());
    }

    @Test
    @DisplayName("Should calculate multi-item estimation totals with GST accurately")
    void testRecalculateEstimationTotals() {
        Estimation estimation = new Estimation();

        EstimationItem item1 = EstimationItem.builder()
                .matchedProduct(cementProduct)
                .requestedQuantity(BigDecimal.valueOf(100)) // 100 * 400 = 40,000 + 7,200 GST
                .build();

        EstimationItem item2 = EstimationItem.builder()
                .matchedProduct(steelProduct)
                .requestedQuantity(BigDecimal.valueOf(500)) // 500 * 70 = 35,000 + 6,300 GST
                .build();

        estimation.addItem(item1);
        estimation.addItem(item2);

        calculationService.recalculateEstimationTotals(estimation);

        // Subtotal = 40,000 + 35,000 = 75,000
        assertEquals(new BigDecimal("75000.00"), estimation.getSubtotal());
        // Tax = 7,200 + 6,300 = 13,500
        assertEquals(new BigDecimal("13500.00"), estimation.getTaxAmount());
        // Grand Total = 75,000 + 13,500 = 88,500
        assertEquals(new BigDecimal("88500.00"), estimation.getGrandTotal());
    }
}
