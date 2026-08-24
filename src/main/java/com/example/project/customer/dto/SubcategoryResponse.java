package com.example.project.customer.dto;

import java.time.LocalDateTime;

public record SubcategoryResponse(Integer subcategoryId, Integer categoryId, String name, String slug,
                                  String imageUrl, boolean active, Integer sortOrder, LocalDateTime createdAt) {
}