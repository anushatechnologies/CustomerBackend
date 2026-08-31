package com.example.project.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request body for setting the available quantity of a product. */
@Getter
@Setter
@NoArgsConstructor
public class StockQuantityUpdateRequest {

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    private Integer stockQty;
}
