package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerQuotationCreateRequest {

    private Object enquiryId;
    private String buyerName;
    private String buyerEmail;
    private String validUntil;

    @Builder.Default
    private List<SellerQuotationItemDto> items = new ArrayList<>();

    private BigDecimal freightCharges;
    private String paymentTerms;
    private String deliveryTimeline;
}
