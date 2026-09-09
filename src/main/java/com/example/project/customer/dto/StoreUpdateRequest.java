package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreUpdateRequest {

    @NotBlank(message = "Store name is required")
    private String name;

    private String logoUrl;
    private String bannerUrl;
    private String description;
    private BigDecimal minOrderValue;
    private Integer serviceRadiusKm;
}
