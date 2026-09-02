package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SellerQuotationRecordResponse {

    private Object id;
    private String quotationNumber;
    private String enquiryId;
    private String buyerName;
    private String buyerEmail;
    private String validUntil;
    private BigDecimal totalAmount;
    private BigDecimal freightCharges;
    private String paymentTerms;
    private String deliveryTimeline;
    private String status;
    private List<SellerQuotationItemDto> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
}
