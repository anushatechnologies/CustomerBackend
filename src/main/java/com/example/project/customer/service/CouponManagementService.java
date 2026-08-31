package com.example.project.customer.service;

import com.example.project.customer.dto.CouponRequest;
import com.example.project.customer.dto.CouponResponse;

import java.util.List;
import java.util.Optional;

/**
 * Service for admin operations: creating, updating, and managing coupons.
 */
public interface CouponManagementService {

    /**
     * Create a new coupon.
     *
     * @param request the coupon creation request
     * @return the created coupon response
     */
    CouponResponse createCoupon(CouponRequest request);

    /**
     * Update an existing coupon.
     *
     * @param couponId the coupon ID
     * @param request the coupon update request
     * @return the updated coupon response
     */
    CouponResponse updateCoupon(Integer couponId, CouponRequest request);

    /**
     * Get a coupon by ID.
     *
     * @param couponId the coupon ID
     * @return the coupon response if found
     */
    Optional<CouponResponse> getCouponById(Integer couponId);

    /**
     * Get a coupon by code.
     *
     * @param code the coupon code
     * @return the coupon response if found
     */
    Optional<CouponResponse> getCouponByCode(String code);

    /**
     * List all coupons.
     *
     * @return list of coupon responses
     */
    List<CouponResponse> listAllCoupons();

    /**
     * Activate a coupon (set status to ACTIVE).
     *
     * @param couponId the coupon ID
     * @return the updated coupon response
     */
    CouponResponse activateCoupon(Integer couponId);

    /**
     * Deactivate a coupon (set status to INACTIVE).
     *
     * @param couponId the coupon ID
     * @return the updated coupon response
     */
    CouponResponse deactivateCoupon(Integer couponId);

    /**
     * Delete a coupon.
     *
     * @param couponId the coupon ID
     */
    void deleteCoupon(Integer couponId);
}
