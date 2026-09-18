package com.example.project.customer.dto;

import com.example.project.customer.entity.DiscountType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class EligibleCouponResponse {
    private String code; private String title; private String description; private DiscountType discountType;
    private BigDecimal discountValue; private BigDecimal minOrderAmount; private BigDecimal maxDiscountAmount;
    private LocalDateTime expiryDate; private boolean isApplicable; private BigDecimal estimatedDiscount; private BigDecimal shortfallAmount;
}
