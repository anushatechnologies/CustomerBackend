package com.example.project.customer.service;

import com.example.project.customer.dto.CouponValidationRequest;
import com.example.project.customer.dto.CouponValidationResponse;
import com.example.project.customer.dto.CouponValidationReason;
import com.example.project.customer.entity.Coupon;
import com.example.project.customer.entity.CouponStatus;
import com.example.project.customer.entity.UserSegment;
import com.example.project.customer.entity.VehicleType;
import com.example.project.customer.repository.CouponRepository;
import com.example.project.customer.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

// Note: For NEW_USERS_ONLY validation, you'll need to inject OrderService or similar
// to check if the user has completed orders. See the isExistingCustomer() method below.

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponValidationServiceImpl implements CouponValidationService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponDiscountCalculator discountCalculator;

    @Override
    public CouponValidationResponse validateCoupon(CouponValidationRequest request, Integer userId) {
        String couponCode = request.getCouponCode().trim().toUpperCase();
        BigDecimal orderFare = request.getOrderFare();
        String vehicleType = request.getVehicleType();

        // 1. Check if coupon exists
        Optional<Coupon> couponOpt = couponRepository.findByCodeIgnoreCase(couponCode);
        if (couponOpt.isEmpty()) {
            return buildInvalidResponse(couponCode, CouponValidationReason.COUPON_NOT_FOUND);
        }

        Coupon coupon = couponOpt.get();

        // 2. Check if coupon is active
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            if (coupon.getStatus() == CouponStatus.EXPIRED) {
                return buildInvalidResponse(couponCode, CouponValidationReason.COUPON_EXPIRED);
            } else if (coupon.getStatus() == CouponStatus.EXHAUSTED) {
                return buildInvalidResponse(couponCode, CouponValidationReason.TOTAL_USAGE_LIMIT_REACHED);
            } else {
                return buildInvalidResponse(couponCode, CouponValidationReason.COUPON_INACTIVE);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // 3. Check if coupon is within validity period
        if (now.isBefore(coupon.getValidFrom())) {
            return buildInvalidResponse(couponCode, CouponValidationReason.COUPON_NOT_YET_VALID);
        }

        if (now.isAfter(coupon.getValidTo())) {
            return buildInvalidResponse(couponCode, CouponValidationReason.COUPON_EXPIRED);
        }

        // 4. Check minimum order value
        if (orderFare != null && orderFare.compareTo(coupon.getMinOrderValue()) < 0) {
            return buildInvalidResponse(couponCode, CouponValidationReason.MIN_ORDER_VALUE_NOT_MET,
                    String.format("This coupon requires a minimum order of ₹%.2f. Add ₹%.2f more to use it.",
                            coupon.getMinOrderValue(), coupon.getMinOrderValue().subtract(orderFare)));
        }

        // 5. Check vehicle type applicability
        if (vehicleType != null && !isVehicleTypeApplicable(coupon, vehicleType)) {
            return buildInvalidResponse(couponCode, CouponValidationReason.VEHICLE_TYPE_NOT_APPLICABLE);
        }

        // 6. Check user segment eligibility
        CouponValidationReason segmentCheckReason = checkUserSegmentEligibility(coupon, userId);
        if (segmentCheckReason != null) {
            return buildInvalidResponse(couponCode, segmentCheckReason);
        }

        // 7. Check total usage limit (this is informational in validation, actual check is at apply time)
        if (coupon.getUsageLimitTotal() != null &&
                coupon.getCurrentUsageCount() >= coupon.getUsageLimitTotal()) {
            return buildInvalidResponse(couponCode, CouponValidationReason.TOTAL_USAGE_LIMIT_REACHED);
        }

        // 8. Check per-user usage limit (this is informational in validation, actual check is at apply time)
        Integer userUsageCount = couponUsageRepository.countUserCouponUsage(coupon.getCouponId(), userId);
        if (userUsageCount >= coupon.getUsageLimitPerUser()) {
            return buildInvalidResponse(couponCode, CouponValidationReason.USER_USAGE_LIMIT_REACHED);
        }

        // All validations passed - calculate discount
        BigDecimal discountAmount = discountCalculator.calculate(coupon, orderFare);

        return CouponValidationResponse.builder()
                .valid(true)
                .couponCode(couponCode)
                .discountAmount(discountAmount)
                .build();
    }

    private boolean isVehicleTypeApplicable(Coupon coupon, String vehicleType) {
        if (coupon.getApplicableVehicleTypes() == null || coupon.getApplicableVehicleTypes().isEmpty()) {
            return true;
        }

        return coupon.getApplicableVehicleTypes().contains(VehicleType.ALL) ||
                coupon.getApplicableVehicleTypes().stream()
                        .anyMatch(vt -> vt.name().equalsIgnoreCase(vehicleType));
    }

    private CouponValidationReason checkUserSegmentEligibility(Coupon coupon, Integer userId) {
        if (coupon.getApplicableUserSegment() == UserSegment.ALL_USERS) {
            return null;
        }

        if (coupon.getApplicableUserSegment() == UserSegment.NEW_USERS_ONLY) {
            // Check if user is a new user (has no completed orders)
            // This is a simplified check - implement based on your Order service
            if (isExistingCustomer(userId)) {
                return CouponValidationReason.USER_SEGMENT_NOT_ELIGIBLE;
            }
            return null;
        }

        if (coupon.getApplicableUserSegment() == UserSegment.SPECIFIC_USER_IDS) {
            if (coupon.getSpecificUserIds() == null || 
                    !coupon.getSpecificUserIds().contains(userId)) {
                return CouponValidationReason.USER_SEGMENT_NOT_ELIGIBLE;
            }
            return null;
        }

        return null;
    }

    private boolean isExistingCustomer(Integer userId) {
        // TODO: Implement based on your Order service - check if user has completed orders
        // For now, return false (everyone is considered new)
        return false;
    }

    private CouponValidationResponse buildInvalidResponse(String couponCode, CouponValidationReason reason) {
        return buildInvalidResponse(couponCode, reason, reason.getMessage());
    }

    private CouponValidationResponse buildInvalidResponse(String couponCode, CouponValidationReason reason, String message) {
        return CouponValidationResponse.builder()
                .valid(false)
                .couponCode(couponCode)
                .reason(reason.name())
                .message(message)
                .build();
    }
}
