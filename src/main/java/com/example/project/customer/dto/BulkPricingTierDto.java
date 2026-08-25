package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkPricingTierDto {
    private Integer tierId;
    private Integer minQty;
    private Integer maxQty;
    private BigDecimal price;
    private Double discountPercentage;

    public BulkPricingTierDto() {
    }

    public BulkPricingTierDto(Integer tierId, Integer minQty, Integer maxQty, BigDecimal price, Double discountPercentage) {
        this.tierId = tierId;
        this.minQty = minQty;
        this.maxQty = maxQty;
        this.price = price;
        this.discountPercentage = discountPercentage;
    }

    public Integer getTierId() {
        return tierId;
    }

    public void setTierId(Integer tierId) {
        this.tierId = tierId;
    }

    public Integer getMinQty() {
        return minQty;
    }

    public void setMinQty(Integer minQty) {
        this.minQty = minQty;
    }

    public Integer getMaxQty() {
        return maxQty;
    }

    public void setMaxQty(Integer maxQty) {
        this.maxQty = maxQty;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(Double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }
}
