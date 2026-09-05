package com.example.project.customer.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalBookingRequest {

    @NotNull(message = "Equipment ID is required")
    private Integer equipmentId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be present or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotBlank(message = "Site address is required")
    private String siteAddress;

    private Boolean operatorRequired;
}
