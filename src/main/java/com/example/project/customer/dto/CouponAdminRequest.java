package com.example.project.customer.dto;

import com.example.project.customer.entity.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponAdminRequest {
    @NotBlank @Size(max = 50) private String code;
    @NotBlank @Size(max = 150) private String title;
    @Size(max = 500) private String description;
    @NotNull private DiscountType discountType;
    @NotNull @DecimalMin(value = "0.01") private BigDecimal discountValue;
    @DecimalMin(value = "0.00") private BigDecimal minOrderAmount;
    @DecimalMin(value = "0.01") private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    @Positive private Integer totalUsageLimit;
    @Positive private Integer perUserLimit;
    private Boolean firstOrderOnly;
    private Boolean isActive;
}
