package com.example.project.customer.dto;

import com.example.project.customer.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @AllArgsConstructor
public class CouponValidationResult {
    private final Coupon coupon;
    private final BigDecimal discount;
}
