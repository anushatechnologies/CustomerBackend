package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerifyRequest {

    @JsonAlias({"order_id"})
    private Integer orderId;

    @JsonAlias({"razorpay_order_id", "order_id"})
    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @JsonAlias({"razorpay_payment_id", "payment_id"})
    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @JsonAlias({"razorpay_signature", "signature"})
    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
}
