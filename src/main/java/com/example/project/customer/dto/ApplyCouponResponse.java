package com.example.project.customer.dto;

import java.math.BigDecimal;

public class ApplyCouponResponse {

    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal newGrandTotal;

    public ApplyCouponResponse() {
    }

    public ApplyCouponResponse(String couponCode, BigDecimal discountAmount, BigDecimal newGrandTotal) {
        this.couponCode = couponCode;
        this.discountAmount = discountAmount;
        this.newGrandTotal = newGrandTotal;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getNewGrandTotal() {
        return newGrandTotal;
    }

    public void setNewGrandTotal(BigDecimal newGrandTotal) {
        this.newGrandTotal = newGrandTotal;
    }
}
