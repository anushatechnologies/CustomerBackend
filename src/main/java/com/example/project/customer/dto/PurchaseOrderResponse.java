package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseOrderResponse {
    private Integer poId;
    private String poNumber;
    private Integer userId;
    private Integer vendorId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDate deliveryDate;
    private String billingAddress;
    private String shippingAddress;
    private String paymentTerms;
    private String notes;
    private String rejectionReason;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private List<PurchaseOrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
