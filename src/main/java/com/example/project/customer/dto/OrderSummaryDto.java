package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSummaryDto {

    private Integer orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private Integer itemCount;
    private String firstItemTitle;
    private String firstItemImage;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedDelivery;

    public OrderSummaryDto() {
    }

    public OrderSummaryDto(Integer orderId, String orderNumber, BigDecimal totalAmount, String orderStatus,
                           String paymentStatus, Integer itemCount, String firstItemTitle,
                           String firstItemImage, LocalDateTime createdAt, LocalDateTime estimatedDelivery) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
        this.itemCount = itemCount;
        this.firstItemTitle = firstItemTitle;
        this.firstItemImage = firstItemImage;
        this.createdAt = createdAt;
        this.estimatedDelivery = estimatedDelivery;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public String getFirstItemTitle() {
        return firstItemTitle;
    }

    public void setFirstItemTitle(String firstItemTitle) {
        this.firstItemTitle = firstItemTitle;
    }

    public String getFirstItemImage() {
        return firstItemImage;
    }

    public void setFirstItemImage(String firstItemImage) {
        this.firstItemImage = firstItemImage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }
}
