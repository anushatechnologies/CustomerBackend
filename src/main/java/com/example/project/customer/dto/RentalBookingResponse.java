package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentalBookingResponse {
    private Integer bookingId;
    private Integer userId;
    private Integer equipmentId;
    private String equipmentName;
    private String category;
    private LocalDate startDate;
    private LocalDate endDate;
    private String siteAddress;
    private Boolean operatorRequired;
    private Integer totalDays;
    private BigDecimal ratePerDay;
    private BigDecimal operatorCost;
    private BigDecimal depositAmount;
    private BigDecimal totalCost;
    private String status;
    private LocalDateTime createdAt;
}
