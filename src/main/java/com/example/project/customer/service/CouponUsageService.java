package com.example.project.customer.service;

import com.example.project.customer.dto.CouponApplyResponse;
import com.example.project.customer.entity.Coupon;
import com.example.project.customer.entity.CouponUsage;

/**
 * Service for recording coupon usage atomically at order confirmation time.
 * Enforces concurrency-safe consumption of coupon usage limits.
 */
public interface CouponUsageService {

    /**
     * Apply (consume) a coupon for a specific order.
     * This performs atomic validation and usage recording to prevent double-redemption.
     * 
     * @param couponCode the coupon code
     * @param userId the user ID
     * @param orderId the order ID
     * @param orderFare the order fare
     * @param vehicleType the vehicle type
     * @return a response indicating success or failure with reason
     */
    CouponApplyResponse applyCoupon(String couponCode, Integer userId, Integer orderId, 
                                     java.math.BigDecimal orderFare, String vehicleType);

    /**
     * Release/refund the usage slot when an order is cancelled.
     * Decrements currentUsageCount and removes the CouponUsage record.
     * 
     * @param orderId the order ID to find associated coupon usage
     */
    void releaseCouponUsage(Integer orderId);

    /**
     * Record a coupon usage after it has been validated and approved for consumption.
     * 
     * @param coupon the coupon entity
     * @param userId the user ID
     * @param orderId the order ID
     * @param discountApplied the discount amount applied
     * @return the CouponUsage record created
     */
    CouponUsage recordUsage(Coupon coupon, Integer userId, Integer orderId, java.math.BigDecimal discountApplied);
}
