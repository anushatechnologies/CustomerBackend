package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RfqRequest {

    @NotBlank(message = "RFQ title is required")
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Product / Material name is required")
    private String productMaterial;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Unit of measurement is required")
    private String unit;

    private String technicalGrade;

    @Builder.Default
    @JsonProperty("mtcRequired")
    private Boolean mtcRequired = true;

    @NotBlank(message = "Delivery location is required")
    private String deliveryLocation;

    private LocalDate requiredByDate;

    private String siteAccess;

    @Builder.Default
    @JsonProperty("craneRequired")
    private Boolean craneRequired = false;

    private BigDecimal targetBudget;

    private String paymentTerms;

    private String specifications;

    private String boqAttachmentUrl;
}
