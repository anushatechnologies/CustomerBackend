package com.example.project.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequest(@NotNull @Positive Integer subcategoryId, @NotBlank String title,
                             String description, @NotNull @DecimalMin("0.00") BigDecimal price,
                             @NotNull @PositiveOrZero Integer stockQty, @NotBlank String unit,
                             String imageUrl, Boolean active) {
}