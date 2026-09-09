package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectCategoryRequest(
        @NotBlank(message = "Rejection reason is required")
        String reason
) {
}
