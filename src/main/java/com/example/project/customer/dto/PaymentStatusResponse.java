package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {

    private Integer paymentId;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private Integer orderId;
    private String orderNumber;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String status; // CREATED, AUTHORIZED, CAPTURED, FAILED, REFUNDED
    private BigDecimal amount;
    private String currency;
    private String purpose;
    private String paymentMethod;
    private String email;
    private String contact;
    private String vpa;
    private String bank;
    private String cardNetwork;
    private String cardLast4;
    private String errorCode;
    private String errorDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
