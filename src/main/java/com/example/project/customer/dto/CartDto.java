package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartDto {

    private Integer cartId;
    private List<CartItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal couponDiscount;
    private BigDecimal totalGst;
    private BigDecimal deliveryCharge;
    private BigDecimal grandTotal;
    private String appliedCoupon;

    public CartDto() {
    }

    public CartDto(Integer cartId, List<CartItemDto> items, BigDecimal subtotal, BigDecimal couponDiscount,
                   BigDecimal totalGst, BigDecimal deliveryCharge, BigDecimal grandTotal, String appliedCoupon) {
        this.cartId = cartId;
        this.items = items;
        this.subtotal = subtotal;
        this.couponDiscount = couponDiscount;
        this.totalGst = totalGst;
        this.deliveryCharge = deliveryCharge;
        this.grandTotal = grandTotal;
        this.appliedCoupon = appliedCoupon;
    }

    public Integer getCartId() {
        return cartId;
    }

    public void setCartId(Integer cartId) {
        this.cartId = cartId;
    }

    public List<CartItemDto> getItems() {
        return items;
    }

    public void setItems(List<CartItemDto> items) {
        this.items = items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getCouponDiscount() {
        return couponDiscount;
    }

    public void setCouponDiscount(BigDecimal couponDiscount) {
        this.couponDiscount = couponDiscount;
    }

    public BigDecimal getTotalGst() {
        return totalGst;
    }

    public void setTotalGst(BigDecimal totalGst) {
        this.totalGst = totalGst;
    }

    public BigDecimal getDeliveryCharge() {
        return deliveryCharge;
    }

    public void setDeliveryCharge(BigDecimal deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public String getAppliedCoupon() {
        return appliedCoupon;
    }

    public void setAppliedCoupon(String appliedCoupon) {
        this.appliedCoupon = appliedCoupon;
    }
}
