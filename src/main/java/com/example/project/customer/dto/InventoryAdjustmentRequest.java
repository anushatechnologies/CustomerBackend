package com.example.project.customer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustmentRequest {

    @NotNull(message = "productId is required")
    private Object productId;

    private Object warehouseId;

    @NotNull(message = "adjustmentType is required")
    private String adjustmentType; // "add", "subtract", "set", etc.

    @NotNull(message = "quantity is required")
    private Integer quantity;

    private String reason;

    public Integer getNumericProductId() {
        if (productId == null) return null;
        if (productId instanceof Number) return ((Number) productId).intValue();
        String str = productId.toString();
        if (str.startsWith("sp_")) {
            return Integer.parseInt(str.substring(3));
        }
        return Integer.parseInt(str);
    }

    public Integer getNumericWarehouseId() {
        if (warehouseId == null) return null;
        if (warehouseId instanceof Number) return ((Number) warehouseId).intValue();
        String str = warehouseId.toString();
        if (str.startsWith("wh_")) {
            return Integer.parseInt(str.substring(3));
        }
        return Integer.parseInt(str);
    }
}
