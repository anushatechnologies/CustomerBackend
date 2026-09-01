package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustmentRequest {
    private String productId;
    private String warehouseId;
    private String adjustmentType; // add, deduct, audit_correction
    private Integer quantity;
    private String reason;
}
