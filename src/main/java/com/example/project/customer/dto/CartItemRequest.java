package com.example.project.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(
        @NotNull(message = "Customer ID must not be null")
        @Positive(message = "Customer ID must be positive")
        Integer customerId,

        @NotNull(message = "Product ID must not be null")
        @Positive(message = "Product ID must be positive")
        Integer productId,

        @NotNull(message = "Quantity must not be null")
        @Positive(message = "Quantity must be greater than 0")
        Integer quantity
) {
}
