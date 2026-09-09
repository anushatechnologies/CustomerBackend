package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchStoreRequest {

    private Integer storeId;
    private String storeSlug;
    private Integer pendingProductId;
    private Integer pendingQuantity;
}
