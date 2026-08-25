package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {
    private Integer cartId;

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    private BigDecimal subtotal;
    private BigDecimal couponDiscount;
    private BigDecimal totalGst;
    private BigDecimal deliveryCharge;
    private BigDecimal grandTotal;
    private String appliedCoupon;
}
