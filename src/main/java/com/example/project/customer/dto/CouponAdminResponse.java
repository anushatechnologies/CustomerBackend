package com.example.project.customer.dto;

import com.example.project.customer.entity.DiscountType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class CouponAdminResponse {
    private Long id; private String code; private String title; private String description; private DiscountType discountType;
    private BigDecimal discountValue; private BigDecimal minOrderAmount; private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate; private LocalDateTime expiryDate; private Integer totalUsageLimit; private Integer usedCount;
    private Integer perUserLimit; private Boolean firstOrderOnly; private Boolean isActive; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
