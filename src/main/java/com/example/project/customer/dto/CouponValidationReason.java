package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponValidationReason {
    COUPON_NOT_FOUND("Coupon not found"),
    COUPON_INACTIVE("Coupon is inactive"),
    COUPON_EXPIRED("Coupon has expired"),
    COUPON_NOT_YET_VALID("Coupon is not yet valid"),
    MIN_ORDER_VALUE_NOT_MET("Minimum order value not met"),
    VEHICLE_TYPE_NOT_APPLICABLE("Vehicle type is not applicable for this coupon"),
    USER_SEGMENT_NOT_ELIGIBLE("You are not eligible for this coupon"),
    TOTAL_USAGE_LIMIT_REACHED("Coupon usage limit has been exhausted"),
    USER_USAGE_LIMIT_REACHED("You have reached the maximum usage limit for this coupon");

    private final String message;
}
