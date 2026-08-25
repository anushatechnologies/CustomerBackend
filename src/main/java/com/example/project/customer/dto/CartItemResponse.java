package com.example.project.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemResponse(
        Integer cartItemId,
        Integer customerId,
        String customerName,
        Integer productId,
        String productTitle,
        BigDecimal price,
        String productUnit,
        String productImageUrl,
        Integer quantity,
        BigDecimal itemTotal,
        LocalDateTime addedAt
) {
}
