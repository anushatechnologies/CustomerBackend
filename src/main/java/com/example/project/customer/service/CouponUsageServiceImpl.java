package com.example.project.customer.service;

import com.example.project.customer.dto.CouponApplyResponse;
import com.example.project.customer.dto.CouponValidationReason;
import com.example.project.customer.entity.Coupon;
import com.example.project.customer.entity.CouponStatus;
import com.example.project.customer.entity.CouponUsage;
import com.example.project.customer.repository.CouponRepository;
import com.example.project.customer.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponUsageServiceImpl implements CouponUsageService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponValidationService couponValidationService;

    @Override
    @Transactional
    public CouponApplyResponse applyCoupon(String couponCode, Integer userId, Integer orderId,
                                           BigDecimal orderFare, String vehicleType) {
        log.info("Applying coupon {} for user {} on order {}", couponCode, userId, orderId);

        try {
            // First, validate the coupon (re-validate at apply time for safety)
            com.example.project.customer.dto.CouponValidationRequest validationRequest =
                    com.example.project.customer.dto.CouponValidationRequest.builder()
                            .couponCode(couponCode)
                            .orderFare(orderFare)
                            .vehicleType(vehicleType)
                            .build();

            com.example.project.customer.dto.CouponValidationResponse validationResponse =
                    couponValidationService.validateCoupon(validationRequest, userId);

            if (!validationResponse.isValid()) {
                log.warn("Coupon validation failed for {}: {}", couponCode, validationResponse.getReason());
                return CouponApplyResponse.builder()
                        .success(false)
                        .couponCode(couponCode)
                        .reason(validationResponse.getReason())
                        .message(validationResponse.getMessage())
                        .build();
            }

            // Get the coupon for atomic update
            Optional<Coupon> couponOpt = couponRepository.findByCodeIgnoreCase(couponCode);
            if (couponOpt.isEmpty()) {
                return buildFailureResponse(couponCode, CouponValidationReason.COUPON_NOT_FOUND);
            }

            Coupon coupon = couponOpt.get();

            // Perform atomic check and increment of usage limit
            // Using a native query would be better, but we'll use JPA here for simplicity
            // In production, consider using native SQL with SELECT FOR UPDATE
            synchronized (coupon) {
                // Re-check limits after acquiring lock
                if (coupon.getUsageLimitTotal() != null &&
                        coupon.getCurrentUsageCount() >= coupon.getUsageLimitTotal()) {
                    coupon.setStatus(CouponStatus.EXHAUSTED);
                    couponRepository.save(coupon);
                    log.warn("Coupon {} exhausted", couponCode);
                    return buildFailureResponse(couponCode, CouponValidationReason.TOTAL_USAGE_LIMIT_REACHED);
                }

                // Check per-user limit
                Integer userUsageCount = couponUsageRepository.countUserCouponUsage(coupon.getCouponId(), userId);
                if (userUsageCount >= coupon.getUsageLimitPerUser()) {
                    log.warn("User {} exceeded per-user limit for coupon {}", userId, couponCode);
                    return buildFailureResponse(couponCode, CouponValidationReason.USER_USAGE_LIMIT_REACHED);
                }

                // Increment usage count
                coupon.setCurrentUsageCount(coupon.getCurrentUsageCount() + 1);

                // Check if coupon is now exhausted after this use
                if (coupon.getUsageLimitTotal() != null &&
                        coupon.getCurrentUsageCount() >= coupon.getUsageLimitTotal()) {
                    coupon.setStatus(CouponStatus.EXHAUSTED);
                }

                couponRepository.save(coupon);
            }

            // Record the usage
            BigDecimal discountAmount = validationResponse.getDiscountAmount();
            recordUsage(coupon, userId, orderId, discountAmount);

            log.info("Coupon {} successfully applied to order {}, discount: {}", couponCode, orderId, discountAmount);
            return CouponApplyResponse.builder()
                    .success(true)
                    .couponCode(couponCode)
                    .discountAmount(discountAmount)
                    .build();

        } catch (Exception e) {
            log.error("Error applying coupon {}: ", couponCode, e);
            return buildFailureResponse(couponCode, CouponValidationReason.COUPON_NOT_FOUND,
                    "An error occurred while applying the coupon. Please try again.");
        }
    }

    @Override
    @Transactional
    public void releaseCouponUsage(Integer orderId) {
        log.info("Releasing coupon usage for order {}", orderId);

        List<CouponUsage> usages = couponUsageRepository.findByOrderId(orderId);

        for (CouponUsage usage : usages) {
            Coupon coupon = usage.getCoupon();

            // Decrement usage count
            coupon.setCurrentUsageCount(Math.max(0, coupon.getCurrentUsageCount() - 1));

            // If coupon was marked as EXHAUSTED, revert to ACTIVE if no longer exhausted
            if (coupon.getStatus() == CouponStatus.EXHAUSTED) {
                if (coupon.getUsageLimitTotal() == null ||
                        coupon.getCurrentUsageCount() < coupon.getUsageLimitTotal()) {
                    coupon.setStatus(CouponStatus.ACTIVE);
                }
            }

            couponRepository.save(coupon);

            // Remove the usage record
            couponUsageRepository.delete(usage);

            log.info("Released coupon usage: coupon {}, order {}, user {}", 
                    coupon.getCouponId(), orderId, usage.getUserId());
        }
    }

    @Override
    @Transactional
    public CouponUsage recordUsage(Coupon coupon, Integer userId, Integer orderId, BigDecimal discountApplied) {
        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .userId(userId)
                .orderId(orderId)
                .discountApplied(discountApplied)
                .build();

        return couponUsageRepository.save(usage);
    }

    private CouponApplyResponse buildFailureResponse(String couponCode, CouponValidationReason reason) {
        return buildFailureResponse(couponCode, reason, reason.getMessage());
    }

    private CouponApplyResponse buildFailureResponse(String couponCode, CouponValidationReason reason, String message) {
        return CouponApplyResponse.builder()
                .success(false)
                .couponCode(couponCode)
                .reason(reason.name())
                .message(message)
                .build();
    }
}
