package com.example.project.customer.dto;

import com.example.project.customer.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SellerDiscountRequest(
        @NotBlank(message = "Discount code is required") String code,
        @NotNull(message = "Discount type is required") DiscountType discountType,
        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be greater than zero") BigDecimal discountValue,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "End date is required") LocalDate endDate,
        @NotNull(message = "Minimum order amount is required")
        @DecimalMin(value = "0.00", message = "Minimum order amount must be zero or more") BigDecimal minimumOrderAmount,
        BigDecimal maxDiscountAmount,
        String description,
        Boolean active
) {
}
