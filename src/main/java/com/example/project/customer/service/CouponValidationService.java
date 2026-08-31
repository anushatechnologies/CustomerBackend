package com.example.project.customer.service;

import com.example.project.customer.dto.CouponValidationRequest;
import com.example.project.customer.dto.CouponValidationResponse;

/**
 * Service for validating coupons at preview time (read-only, no usage consumed).
 */
public interface CouponValidationService {
    
    /**
     * Validate a coupon without consuming its usage.
     * This is used for preview/preview scenarios - no usage is recorded.
     *
     * @param request the validation request containing coupon code and order details
     * @param userId the ID of the user applying the coupon
     * @return a validation response with result and discount amount (if valid)
     */
    CouponValidationResponse validateCoupon(CouponValidationRequest request, Integer userId);
}
