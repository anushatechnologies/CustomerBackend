package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAvailability {
    private Integer productId;
    private int availableQty;
    private boolean inStock;
    private String reason; // e.g. null, "UNAVAILABLE_NO_VENDOR_STOCK", "CHECKOUT_DISABLED", "OUT_OF_STOCK", "PRODUCT_INACTIVE"
}
