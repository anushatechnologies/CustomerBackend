package com.example.project.customer.dto;

import com.example.project.customer.entity.DiscountStatus;
import com.example.project.customer.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDiscountResponse {
    private Integer discountId;
    private Integer sellerId;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private DiscountStatus status;
    private Boolean active;
    private String rejectionReason;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
