package com.example.project.customer.dto.settlement;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRuleUpdateRequest {

    /**
     * Optional category ID. If null, this rule acts as the global fallback holding fee.
     */
    private Integer categoryId;

    /**
     * Holding fee percentage (X%) that the seller receives.
     */
    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.00", message = "Rate must be at least 0.00")
    @DecimalMax(value = "100.00", message = "Rate cannot exceed 100.00")
    private BigDecimal rate;

    private Boolean isActive;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;
}
