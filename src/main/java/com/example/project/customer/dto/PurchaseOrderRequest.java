package com.example.project.customer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequest {

    @NotNull(message = "Vendor ID is required")
    private Integer vendorId;

    private LocalDate deliveryDate;

    private String billingAddress;

    private String shippingAddress;

    private String paymentTerms;

    private String notes;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<PurchaseOrderItemRequest> items;
}
