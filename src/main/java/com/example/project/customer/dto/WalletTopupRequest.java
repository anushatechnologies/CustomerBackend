package com.example.project.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTopupRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum topup amount is 1.00")
    private BigDecimal amount;

    private String description;
}
