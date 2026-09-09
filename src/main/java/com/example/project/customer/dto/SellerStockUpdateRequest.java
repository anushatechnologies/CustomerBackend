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
public class SellerStockUpdateRequest {

    private Integer sellerId;

    @NotNull(message = "stockQty is required")
    private Integer stockQty;
}
