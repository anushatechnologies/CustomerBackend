package com.example.project.customer.service;

import com.example.project.customer.dto.settlement.ItemSettlementSnapshot;
import com.example.project.customer.dto.settlement.RefundSettlementSnapshot;
import com.example.project.customer.entity.CommissionRule;
import com.example.project.customer.repository.CommissionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementCalculationServiceImpl implements SettlementCalculationService {

    private final CommissionRuleRepository commissionRuleRepository;
    private static final BigDecimal TCS_RATE = BigDecimal.valueOf(0.01); // 1% TCS
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    public ItemSettlementSnapshot calculateItemSettlement(
            boolean isHinchStore,
            BigDecimal preDiscountAmount,
            BigDecimal allocatedDiscount,
            Integer categoryId) {

        if (preDiscountAmount == null || preDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Pre-discount item amount must be non-null and non-negative");
        }

        BigDecimal discount = (allocatedDiscount != null && allocatedDiscount.compareTo(BigDecimal.ZERO) > 0)
                ? allocatedDiscount.min(preDiscountAmount).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal effectiveSellingAmount = preDiscountAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);

        if (!isHinchStore) {
            // Vendor's own product:
            // All coupons are platform-funded: Vendor is paid 100% of PRE-discount item amount!
            BigDecimal sellerPayable = preDiscountAmount.setScale(2, RoundingMode.HALF_UP);
            // Hinch keeps remainder = customer paid (effectiveSellingAmount) - vendor payout (sellerPayable)
            // = (preDiscount - discount) - preDiscount = -discount (platform subsidy)
            BigDecimal hinchKeeps = effectiveSellingAmount.subtract(sellerPayable).setScale(2, RoundingMode.HALF_UP);
            BigDecimal commissionRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            // 1% TCS computed on vendor's payable base
            BigDecimal tcsAmount = sellerPayable.multiply(TCS_RATE).setScale(2, RoundingMode.HALF_UP);

            boolean marginDeficit = hinchKeeps.compareTo(BigDecimal.ZERO) < 0;
            if (marginDeficit) {
                log.info("Platform funded coupon subsidy on vendor item: discount={}, hinchKeeps={}", discount, hinchKeeps);
            }

            return ItemSettlementSnapshot.builder()
                    .isHinchStore(false)
                    .preDiscountAmount(preDiscountAmount.setScale(2, RoundingMode.HALF_UP))
                    .allocatedDiscount(discount)
                    .effectiveSellingAmount(effectiveSellingAmount)
                    .commissionRate(commissionRate)
                    .sellerPayable(sellerPayable)
                    .hinchKeeps(hinchKeeps)
                    .tcsAmount(tcsAmount)
                    .marginDeficit(marginDeficit)
                    .build();
        }

        // HinchStore item:
        LocalDateTime now = LocalDateTime.now();

        // Ensure only one active global rule can apply
        List<CommissionRule> activeGlobals = commissionRuleRepository.findActiveGlobalRules(now);
        if (activeGlobals.size() > 1) {
            throw new IllegalStateException("Multiple (" + activeGlobals.size()
                    + ") active global commission rules found at " + now
                    + ". Ensure only one active global rule applies.");
        }

        List<CommissionRule> applicableRules = commissionRuleRepository.findApplicableRules(categoryId, now);
        if (applicableRules.isEmpty()) {
            throw new IllegalStateException("No active commission rule exists for categoryId " + categoryId
                    + " or global scope at " + now + ". Missing rule must throw.");
        }

        CommissionRule rule = applicableRules.get(0);
        BigDecimal rate = rule.getRate().setScale(2, RoundingMode.HALF_UP);

        // HinchStore holding fee = X% of PRE-discount item amount (Hinch absorbs the coupon)
        BigDecimal sellerPayable = preDiscountAmount
                .multiply(rate)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

        // Hinch keeps remainder = customer paid (effectiveSellingAmount) - vendor holding fee (sellerPayable)
        BigDecimal hinchKeeps = effectiveSellingAmount.subtract(sellerPayable).setScale(2, RoundingMode.HALF_UP);

        boolean marginDeficit = hinchKeeps.compareTo(BigDecimal.ZERO) < 0;
        if (marginDeficit) {
            log.warn("MARGIN DEFICIT on HinchStore item: Vendor holding fee ({}) exceeds customer paid ({}), Hinch keeps negative ({})",
                    sellerPayable, effectiveSellingAmount, hinchKeeps);
        }

        // TCS is disabled for HinchStore items until confirmed
        BigDecimal tcsAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return ItemSettlementSnapshot.builder()
                .isHinchStore(true)
                .preDiscountAmount(preDiscountAmount.setScale(2, RoundingMode.HALF_UP))
                .allocatedDiscount(discount)
                .effectiveSellingAmount(effectiveSellingAmount)
                .commissionRate(rate)
                .sellerPayable(sellerPayable)
                .hinchKeeps(hinchKeeps)
                .tcsAmount(tcsAmount)
                .marginDeficit(marginDeficit)
                .build();
    }

    @Override
    public RefundSettlementSnapshot calculateRefundSettlement(
            BigDecimal originalSellerPayable,
            BigDecimal originalHinchKeeps,
            int totalQuantity,
            int refundQuantity) {

        if (totalQuantity <= 0 || refundQuantity <= 0 || refundQuantity > totalQuantity) {
            throw new IllegalArgumentException("Invalid refund quantity: total=" + totalQuantity + ", refund=" + refundQuantity);
        }

        BigDecimal fraction = BigDecimal.valueOf(refundQuantity)
                .divide(BigDecimal.valueOf(totalQuantity), 6, RoundingMode.HALF_UP);

        BigDecimal clawbackPayable = (originalSellerPayable != null)
                ? originalSellerPayable.multiply(fraction).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal reversalHinchKeeps = (originalHinchKeeps != null)
                ? originalHinchKeeps.multiply(fraction).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return RefundSettlementSnapshot.builder()
                .totalQuantity(totalQuantity)
                .refundQuantity(refundQuantity)
                .clawbackSellerPayable(clawbackPayable)
                .reversalHinchKeeps(reversalHinchKeeps)
                .build();
    }
}
