package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponApplyResponse {
    private boolean success;
    private String couponCode;
    private BigDecimal discountAmount;
    private String reason; // Reason code for failed applications
    private String message; // User-friendly message
}
