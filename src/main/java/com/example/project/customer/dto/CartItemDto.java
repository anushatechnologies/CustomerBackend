package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemDto {
    private Integer cartItemId;
    private Integer productId;
    private String title;
    private String imageUrl;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal originalPrice;
    private String appliedTier;
    private Double gstRate;
    private BigDecimal lineTotal;
    private BigDecimal lineGst;

    public CartItemDto() {
    }

    public CartItemDto(Integer cartItemId, Integer productId, String title, String imageUrl,
                       Integer quantity, String unit, BigDecimal unitPrice, BigDecimal originalPrice,
                       String appliedTier, Double gstRate, BigDecimal lineTotal, BigDecimal lineGst) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.originalPrice = originalPrice;
        this.appliedTier = appliedTier;
        this.gstRate = gstRate;
        this.lineTotal = lineTotal;
        this.lineGst = lineGst;
    }

    public Integer getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(Integer cartItemId) {
        this.cartItemId = cartItemId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getAppliedTier() {
        return appliedTier;
    }

    public void setAppliedTier(String appliedTier) {
        this.appliedTier = appliedTier;
    }

    public Double getGstRate() {
        return gstRate;
    }

    public void setGstRate(Double gstRate) {
        this.gstRate = gstRate;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public BigDecimal getLineGst() {
        return lineGst;
    }

    public void setLineGst(BigDecimal lineGst) {
        this.lineGst = lineGst;
    }
}
