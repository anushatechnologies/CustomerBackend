package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class QuotationRequest {

    @NotNull(message = "Vendor ID is required")
    private Integer vendorId;

    @NotNull(message = "Vendor name is required")
    private String vendorName;

    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;

    private Integer deliveryLeadTimeDays;
    private String paymentTermsOffered;

    @Builder.Default
    @JsonProperty("mtcIncluded")
    private Boolean mtcIncluded = true;

    @Builder.Default
    @JsonProperty("freightIncluded")
    private Boolean freightIncluded = true;

    private LocalDateTime validUntil;
    private Double vendorRating;
}
