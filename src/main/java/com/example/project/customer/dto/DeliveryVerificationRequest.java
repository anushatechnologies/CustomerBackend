package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryVerificationRequest {

    @NotBlank(message = "Delivery OTP is required")
    private String otp;

    private BigDecimal codAmountCollected;

    private String paymentReference;
}
