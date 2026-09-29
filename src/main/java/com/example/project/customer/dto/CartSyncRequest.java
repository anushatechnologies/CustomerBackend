package com.example.project.customer.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartSyncRequest {

    @Builder.Default
    @Valid
    private List<CartItemRequest> items = new ArrayList<>();

    private Integer targetStoreId;
}
