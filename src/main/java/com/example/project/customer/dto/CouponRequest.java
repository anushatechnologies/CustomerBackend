package com.example.project.customer.dto;

import com.example.project.customer.entity.DiscountType;
import com.example.project.customer.entity.UserSegment;
import com.example.project.customer.entity.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0", inclusive = false, message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0", inclusive = false, message = "Max discount amount must be greater than 0")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Minimum order value is required")
    @DecimalMin(value = "0", message = "Minimum order value must be non-negative")
    private BigDecimal minOrderValue;

    @Builder.Default
    private List<VehicleType> applicableVehicleTypes = List.of(VehicleType.ALL);

    @Builder.Default
    private UserSegment applicableUserSegment = UserSegment.ALL_USERS;

    private List<Integer> specificUserIds;

    @NotNull(message = "Valid from date is required")
    private LocalDateTime validFrom;

    @NotNull(message = "Valid to date is required")
    private LocalDateTime validTo;

    private Integer usageLimitTotal;

    @Builder.Default
    private Integer usageLimitPerUser = 1;

    private String createdBy;
}
