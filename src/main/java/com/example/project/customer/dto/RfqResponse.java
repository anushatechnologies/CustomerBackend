package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RfqResponse {
    private Integer rfqId;
    private String rfqNumber;
    private String title;
    private String category;
    private String productMaterial;
    private Integer quantity;
    private String unit;
    private String technicalGrade;

    @JsonProperty("mtcRequired")
    private Boolean mtcRequired;

    private String deliveryLocation;
    private LocalDate requiredByDate;
    private String siteAccess;

    @JsonProperty("craneRequired")
    private Boolean craneRequired;

    private BigDecimal targetBudget;
    private String paymentTerms;
    private String specifications;
    private String boqAttachmentUrl;
    private String status;
    private Integer quotesCount;
    private LocalDateTime createdAt;
}
