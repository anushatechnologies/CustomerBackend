package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderGroupResponse {
    private Integer id;
    private String groupOrderNumber;
    private Integer customerId;
    private String customerName;
    private Integer addressId;
    private String deliveryLocation;
    private Boolean orderForSomeoneElse;
    private String recipientName;
    private String recipientPhone;

    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private String couponCode;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal totalGst;
    private BigDecimal totalFreight;
    private BigDecimal totalCraneCharge;
    private BigDecimal grandTotal;

    private String paymentMethod;
    private String paymentStatus;
    private String groupStatus;

    @Builder.Default
    private List<OrderResponse> orders = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
