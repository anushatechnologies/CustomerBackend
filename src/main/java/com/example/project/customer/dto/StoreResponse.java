package com.example.project.customer.dto;

import com.example.project.customer.entity.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {

    private Integer storeId;
    private Integer sellerId;
    private String sellerName;
    private String sellerCompanyName;
    private String name;
    private String slug;
    private String logoUrl;
    private String bannerUrl;
    private String description;
    private StoreStatus status;
    private BigDecimal minOrderValue;
    private Integer serviceRadiusKm;
    private BigDecimal commissionRate;
    private Double rating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
