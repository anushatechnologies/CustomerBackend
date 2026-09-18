package com.example.project.customer.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class CouponUsageResponse {
    private Long id; private Integer customerId; private String customerName; private Integer orderId; private String orderNumber;
    private BigDecimal discountApplied; private LocalDateTime usedAt;
}
