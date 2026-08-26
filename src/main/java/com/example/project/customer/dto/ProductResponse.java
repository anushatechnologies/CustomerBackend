package com.example.project.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(Integer productId, Integer subcategoryId, String title, String description,
                              BigDecimal price, Integer stockQty, String unit, String imageUrl,
                              boolean active, String approvalStatus, String status, String rejectionReason,
                              LocalDateTime createdAt) {
}