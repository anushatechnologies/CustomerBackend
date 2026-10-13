package com.example.project.customer.dto.consignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HinchStoreInventoryResponse {

    private Integer id;
    private Integer productId;
    private String productTitle;
    private Integer vendorStoreId;
    private String vendorStoreName;
    private Integer vendorSellerId;
    private String vendorSellerName;
    private Integer qty;
    private Integer reservedQty;
    private Integer availableQty;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
