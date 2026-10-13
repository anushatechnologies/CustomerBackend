package com.example.project.customer.dto.consignment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HinchStoreInventoryRequest {

    @NotNull(message = "productId is required")
    private Integer productId;

    @NotNull(message = "vendorStoreId is required")
    private Integer vendorStoreId;

    @NotNull(message = "qty is required")
    @Min(value = 0, message = "qty must be greater than or equal to 0")
    private Integer qty;

    private Boolean isActive;
}
