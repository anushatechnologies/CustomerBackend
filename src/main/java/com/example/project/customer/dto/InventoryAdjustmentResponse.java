package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustmentResponse {
    private String productId;
    private String warehouseId;
    private String adjustmentType;
    private Integer quantity;
    private Integer previousStock;
    private Integer newStock;
    private LocalDateTime adjustedAt;
}
