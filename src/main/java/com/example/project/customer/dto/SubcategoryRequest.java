package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record SubcategoryRequest(@NotNull @Positive Integer categoryId, @NotBlank String name,
                                 @NotBlank String slug, String imageUrl, @NotNull Boolean active,
                                 @NotNull @PositiveOrZero Integer sortOrder) {
}