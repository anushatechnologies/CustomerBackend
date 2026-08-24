package com.example.project.customer.dto;

import java.time.LocalDateTime;

public record CategoryResponse(Integer categoryId, String name, String slug, String imageUrl,
                               boolean active, Integer sortOrder, LocalDateTime createdAt) {
}