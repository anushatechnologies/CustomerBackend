package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record DiscountRejectionRequest(@NotBlank(message = "Rejection reason is required") String reason) {
}
