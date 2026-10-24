package com.example.project.customer.service;

import com.example.project.customer.dto.settlement.ItemSettlementSnapshot;
import com.example.project.customer.dto.settlement.RefundSettlementSnapshot;
import com.example.project.customer.entity.CommissionRule;
import com.example.project.customer.repository.CommissionRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementCalculationServiceTest {

    @Mock
    private CommissionRuleRepository commissionRuleRepository;

    private SettlementCalculationServiceImpl settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementCalculationServiceImpl(commissionRuleRepository);
    }

    @Test
    @DisplayName("1. Vendor Own Product: Platform funds coupon! 100% of PRE-discount amount paid to vendor, 0% commission, Hinch absorbs discount")
    void testVendorOwnProduct_SettlesFullDiscountedAmount() {
        BigDecimal preDiscount = new BigDecimal("1000.00");
        BigDecimal couponDiscount = new BigDecimal("150.00");

        ItemSettlementSnapshot snapshot = settlementService.calculateItemSettlement(
                false, // Vendor-own product
                preDiscount,
                couponDiscount,
                5
        );

        assertThat(snapshot.isHinchStore()).isFalse();
        assertThat(snapshot.preDiscountAmount()).isEqualByComparingTo("1000.00");
        assertThat(snapshot.allocatedDiscount()).isEqualByComparingTo("150.00");
        assertThat(snapshot.effectiveSellingAmount()).isEqualByComparingTo("850.00");
        assertThat(snapshot.commissionRate()).isEqualByComparingTo("0.00");
        // Vendor is paid on PRE-discount amount (platform subsidizes the coupon)
        assertThat(snapshot.sellerPayable()).isEqualByComparingTo("1000.00");
        // Hinch absorbs the discount = -150.00
        assertThat(snapshot.hinchKeeps()).isEqualByComparingTo("-150.00");
        // Exact reconciliation: sellerPayable + hinchKeeps == effectiveSellingAmount
        assertThat(snapshot.sellerPayable().add(snapshot.hinchKeeps())).isEqualByComparingTo(snapshot.effectiveSellingAmount());
        // 1% TCS computed on vendor base (1000 * 0.01 = 10.00)
        assertThat(snapshot.tcsAmount()).isEqualByComparingTo("10.00");
        assertThat(snapshot.marginDeficit()).isTrue();
    }

    @Test
    @DisplayName("2. HinchStore Product: Vendor holding fee is X% of PRE-discount amount; Hinch absorbs coupon")
    void testHinchStoreProduct_HoldingFeePreDiscount_HinchAbsorbsCoupon() {
        BigDecimal preDiscount = new BigDecimal("1000.00");
        BigDecimal couponDiscount = new BigDecimal("100.00"); // Customer pays 900.00

        CommissionRule rule = CommissionRule.builder()
                .ruleId(1)
                .rate(new BigDecimal("5.00")) // 5% holding fee
                .isActive(true)
                .build();

        when(commissionRuleRepository.findActiveGlobalRules(any(LocalDateTime.class)))
                .thenReturn(List.of(rule));
        when(commissionRuleRepository.findApplicableRules(eq(2), any(LocalDateTime.class)))
                .thenReturn(List.of(rule));

        ItemSettlementSnapshot snapshot = settlementService.calculateItemSettlement(
                true, // HinchStore product
                preDiscount,
                couponDiscount,
                2
        );

        assertThat(snapshot.isHinchStore()).isTrue();
        assertThat(snapshot.preDiscountAmount()).isEqualByComparingTo("1000.00");
        assertThat(snapshot.allocatedDiscount()).isEqualByComparingTo("100.00");
        assertThat(snapshot.effectiveSellingAmount()).isEqualByComparingTo("900.00");
        assertThat(snapshot.commissionRate()).isEqualByComparingTo("5.00");
        // Holding fee = 5% of PRE-discount (1000 * 0.05 = 50.00)
        assertThat(snapshot.sellerPayable()).isEqualByComparingTo("50.00");
        // Hinch keeps remainder = customer paid (900.00) - vendor fee (50.00) = 850.00
        assertThat(snapshot.hinchKeeps()).isEqualByComparingTo("850.00");
        // TCS disabled for HinchStore items until confirmed
        assertThat(snapshot.tcsAmount()).isEqualByComparingTo("0.00");
        assertThat(snapshot.marginDeficit()).isFalse();
    }

    @Test
    @DisplayName("3. Rounding and fractional rates: 3.33% holding fee on 19.99 preserves scale and rounding")
    void testHinchStoreProduct_FractionalRateRounding() {
        BigDecimal preDiscount = new BigDecimal("19.99");
        BigDecimal zeroDiscount = BigDecimal.ZERO;

        CommissionRule rule = CommissionRule.builder()
                .ruleId(2)
                .rate(new BigDecimal("3.33"))
                .isActive(true)
                .build();

        when(commissionRuleRepository.findActiveGlobalRules(any(LocalDateTime.class)))
                .thenReturn(List.of(rule));
        when(commissionRuleRepository.findApplicableRules(eq(null), any(LocalDateTime.class)))
                .thenReturn(List.of(rule));

        ItemSettlementSnapshot snapshot = settlementService.calculateItemSettlement(
                true,
                preDiscount,
                zeroDiscount,
                null
        );

        // 19.99 * 0.0333 = 0.665667 -> rounds HALF_UP to 0.67
        assertThat(snapshot.sellerPayable()).isEqualByComparingTo("0.67");
        // Hinch keeps 19.99 - 0.67 = 19.32
        assertThat(snapshot.hinchKeeps()).isEqualByComparingTo("19.32");
        assertThat(snapshot.sellerPayable().add(snapshot.hinchKeeps())).isEqualByComparingTo(preDiscount);
    }

    @Test
    @DisplayName("4. Missing commission rule throws IllegalStateException immediately (no silent fallback)")
    void testHinchStoreProduct_MissingRuleThrows() {
        when(commissionRuleRepository.findActiveGlobalRules(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commissionRuleRepository.findApplicableRules(eq(99), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () ->
                settlementService.calculateItemSettlement(
                        true,
                        new BigDecimal("500.00"),
                        BigDecimal.ZERO,
                        99
                )
        );
    }

    @Test
    @DisplayName("5. Multiple active global rules throw IllegalStateException")
    void testHinchStoreProduct_MultipleGlobalRulesThrows() {
        CommissionRule rule1 = CommissionRule.builder().ruleId(1).rate(new BigDecimal("5.00")).isActive(true).build();
        CommissionRule rule2 = CommissionRule.builder().ruleId(2).rate(new BigDecimal("6.00")).isActive(true).build();

        when(commissionRuleRepository.findActiveGlobalRules(any(LocalDateTime.class)))
                .thenReturn(List.of(rule1, rule2));

        assertThrows(IllegalStateException.class, () ->
                settlementService.calculateItemSettlement(
                        true,
                        new BigDecimal("500.00"),
                        BigDecimal.ZERO,
                        null
                )
        );
    }

    @Test
    @DisplayName("6. Margin Deficit: When vendor holding fee exceeds customer paid, flag marginDeficit=true")
    void testHinchStoreProduct_MarginDeficitDetected() {
        // High discount so customer paid is less than holding fee:
        // Item is 100.00, 80% holding fee = 80.00, coupon discount = 90.00 (customer paid 10.00)
        BigDecimal preDiscount = new BigDecimal("100.00");
        BigDecimal heavyCoupon = new BigDecimal("90.00"); // Customer paid 10.00

        CommissionRule rule = CommissionRule.builder()
                .ruleId(3)
                .rate(new BigDecimal("80.00")) // 80% fee
                .isActive(true)
                .build();

        when(commissionRuleRepository.findActiveGlobalRules(any(LocalDateTime.class)))
                .thenReturn(List.of(rule));
        when(commissionRuleRepository.findApplicableRules(eq(1), any(LocalDateTime.class)))
                .thenReturn(List.of(rule));

        ItemSettlementSnapshot snapshot = settlementService.calculateItemSettlement(
                true,
                preDiscount,
                heavyCoupon,
                1
        );

        // Vendor gets 80.00
        assertThat(snapshot.sellerPayable()).isEqualByComparingTo("80.00");
        // Hinch keeps 10.00 - 80.00 = -70.00
        assertThat(snapshot.hinchKeeps()).isEqualByComparingTo("-70.00");
        assertThat(snapshot.marginDeficit()).isTrue();
    }

    @Test
    @DisplayName("7. Partial refund pro-rating: 2 out of 5 units refunded claws back exactly 2/5 of payable and hinch keeps")
    void testPartialRefundProRating() {
        BigDecimal sellerPayable = new BigDecimal("250.00");
        BigDecimal hinchKeeps = new BigDecimal("750.00");

        RefundSettlementSnapshot refund = settlementService.calculateRefundSettlement(
                sellerPayable,
                hinchKeeps,
                5, // total quantity
                2  // refunded quantity
        );

        // 250 * (2/5) = 100.00
        assertThat(refund.clawbackSellerPayable()).isEqualByComparingTo("100.00");
        // 750 * (2/5) = 300.00
        assertThat(refund.reversalHinchKeeps()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("8. Category-specific rule overrides global rule")
    void testCategorySpecificRule_OverridesGlobalRule() {
        CommissionRule globalRule = CommissionRule.builder()
                .ruleId(1)
                .rate(new BigDecimal("5.00"))
                .isActive(true)
                .build();
        CommissionRule categoryRule = CommissionRule.builder()
                .ruleId(2)
                .rate(new BigDecimal("8.50"))
                .isActive(true)
                .build();

        // Applicable rules query orders category-specific rule first
        when(commissionRuleRepository.findActiveGlobalRules(any(LocalDateTime.class)))
                .thenReturn(List.of(globalRule));
        when(commissionRuleRepository.findApplicableRules(eq(36), any(LocalDateTime.class)))
                .thenReturn(List.of(categoryRule, globalRule));

        ItemSettlementSnapshot snapshot = settlementService.calculateItemSettlement(
                true,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                36
        );

        // Uses category-specific rate (8.50%) instead of global (5.00%)
        assertThat(snapshot.commissionRate()).isEqualByComparingTo("8.50");
        assertThat(snapshot.sellerPayable()).isEqualByComparingTo("85.00");
        assertThat(snapshot.hinchKeeps()).isEqualByComparingTo("915.00");
    }

    @Test
    @DisplayName("9. Inactive, future, and expired rules are rejected (throws IllegalStateException)")
    void testInactiveFutureAndExpiredRules_AreRejected() {
        // When repository finds no active rule valid for 'now', it returns empty
        when(commissionRuleRepository.findActiveGlobalRules(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commissionRuleRepository.findApplicableRules(eq(36), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> settlementService.calculateItemSettlement(
                        true,
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        36
                )
        );
    }

    @Test
    @DisplayName("10. Vendor-owned product never queries commission rules repository")
    void testVendorOwnedProduct_NeverQueriesCommissionRules() {
        ItemSettlementSnapshot snapshot = settlementService.calculateItemSettlement(
                false,
                new BigDecimal("500.00"),
                new BigDecimal("50.00"),
                36
        );

        assertThat(snapshot.isHinchStore()).isFalse();
        assertThat(snapshot.commissionRate()).isEqualByComparingTo("0.00");
        assertThat(snapshot.sellerPayable()).isEqualByComparingTo("500.00");
        assertThat(snapshot.hinchKeeps()).isEqualByComparingTo("-50.00");

        org.mockito.Mockito.verifyNoInteractions(commissionRuleRepository);
    }
}
