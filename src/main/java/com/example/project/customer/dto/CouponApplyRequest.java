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
public class CouponApplyRequest {
    private String couponCode;
    private Integer orderId;
    private Integer userId;
    private BigDecimal orderFare;
    private String vehicleType;
}
