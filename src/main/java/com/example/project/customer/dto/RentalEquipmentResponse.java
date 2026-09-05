package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentalEquipmentResponse {
    private Integer equipmentId;
    private String name;
    private String category;
    private String model;
    private String specifications;
    private BigDecimal dailyRate;
    private BigDecimal weeklyRate;
    private BigDecimal monthlyRate;
    private BigDecimal depositAmount;
    private String imageUrl;
    private String location;
    private Boolean operatorAvailable;
    private BigDecimal operatorDailyCharge;
    private Boolean available;
}
