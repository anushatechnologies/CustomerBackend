package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationResponse {
    private Integer quoteId;
    private Integer rfqId;
    private Integer vendorId;
    private String vendorName;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer deliveryLeadTimeDays;
    private String paymentTermsOffered;

    @JsonProperty("mtcIncluded")
    private boolean mtcIncluded;

    @JsonProperty("freightIncluded")
    private boolean freightIncluded;

    private LocalDateTime validUntil;
    private Double vendorRating;
    private String status;
}
