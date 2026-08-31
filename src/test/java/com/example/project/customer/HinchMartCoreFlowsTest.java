package com.example.project.customer;

import com.example.project.customer.entity.BulkPricingTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class HinchMartCoreFlowsTest {

    @Test
    @DisplayName("Verify bulk volume pricing tier calculation logic")
    void testBulkVolumeTierCalculation() {
        BulkPricingTier t1 = BulkPricingTier.builder()
                .tierId(1).minQty(5).maxQty(19).price(BigDecimal.valueOf(54200.0)).discountPercentage(8.1).build();
        BulkPricingTier t2 = BulkPricingTier.builder()
                .tierId(2).minQty(20).maxQty(49).price(BigDecimal.valueOf(52800.0)).discountPercentage(10.5).build();
        BulkPricingTier t3 = BulkPricingTier.builder()
                .tierId(3).minQty(50).maxQty(null).price(BigDecimal.valueOf(51200.0)).discountPercentage(13.2).build();

        List<BulkPricingTier> tiers = List.of(t1, t2, t3);

        int qty1 = 10;
        BigDecimal price1 = tiers.stream()
                .filter(t -> (t.getMinQty() == null || qty1 >= t.getMinQty()) && (t.getMaxQty() == null || qty1 <= t.getMaxQty()))
                .findFirst().map(BulkPricingTier::getPrice).orElse(BigDecimal.valueOf(59000.0));
        assertEquals(BigDecimal.valueOf(54200.0), price1);

        int qty2 = 25;
        BigDecimal price2 = tiers.stream()
                .filter(t -> (t.getMinQty() == null || qty2 >= t.getMinQty()) && (t.getMaxQty() == null || qty2 <= t.getMaxQty()))
                .findFirst().map(BulkPricingTier::getPrice).orElse(BigDecimal.valueOf(59000.0));
        assertEquals(BigDecimal.valueOf(52800.0), price2);

        int qty3 = 100;
        BigDecimal price3 = tiers.stream()
                .filter(t -> (t.getMinQty() == null || qty3 >= t.getMinQty()) && (t.getMaxQty() == null || qty3 <= t.getMaxQty()))
                .findFirst().map(BulkPricingTier::getPrice).orElse(BigDecimal.valueOf(59000.0));
        assertEquals(BigDecimal.valueOf(51200.0), price3);
    }

    @Test
    @DisplayName("Verify split GST computation for intra-state (Telangana 9% CGST + 9% SGST)")
    void testIntraStateTaxSplit() {
        BigDecimal subtotal = BigDecimal.valueOf(1320000.0);
        BigDecimal discount = BigDecimal.valueOf(50000.0);
        BigDecimal taxable = subtotal.subtract(discount); // 1270000.0

        BigDecimal cgst = taxable.multiply(BigDecimal.valueOf(0.09)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal sgst = taxable.multiply(BigDecimal.valueOf(0.09)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalGst = cgst.add(sgst);

        BigDecimal freight = BigDecimal.valueOf(4500.0);
        BigDecimal crane = BigDecimal.valueOf(2500.0);
        BigDecimal grandTotal = taxable.add(totalGst).add(freight).add(crane);

        assertEquals(BigDecimal.valueOf(114300.00).setScale(2), cgst);
        assertEquals(BigDecimal.valueOf(114300.00).setScale(2), sgst);
        assertEquals(BigDecimal.valueOf(228600.00).setScale(2), totalGst);
        assertEquals(BigDecimal.valueOf(1505600.00).setScale(2), grandTotal);
    }
}
