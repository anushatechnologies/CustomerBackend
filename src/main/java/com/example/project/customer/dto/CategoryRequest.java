package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CategoryRequest(@NotBlank String name, @NotBlank String slug, String imageUrl,
                              @NotNull Boolean active, @NotNull @PositiveOrZero Integer sortOrder) {
}