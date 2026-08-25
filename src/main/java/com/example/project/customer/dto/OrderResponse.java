package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    private Integer orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal totalGst;
    private BigDecimal freightCharge;
    private BigDecimal craneUnloadingCharge;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private String poNumber;
    private String deliverySlot;
    private String deliveryInstructions;
    private Boolean requiresCraneUnloading;
    private Integer itemCount;
    private String firstItemTitle;
    private String firstItemImage;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedDelivery;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderItemDto {
        private Integer orderItemId;
        private Integer productId;
        private String title;
        private String imageUrl;
        private Integer quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal originalPrice;
        private String appliedTier;
        private BigDecimal gstRate;
        private BigDecimal lineTotal;
        private BigDecimal lineGst;
    }
}
