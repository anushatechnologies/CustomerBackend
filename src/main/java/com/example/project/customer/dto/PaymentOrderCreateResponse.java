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
public class PaymentOrderCreateResponse {

    private String razorpayOrderId;
    private String keyId;
    private BigDecimal amount;
    private Long amountInPaise;
    private String currency;
    private Integer orderId;
    private String orderNumber;
    private String purpose;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String description;
}
