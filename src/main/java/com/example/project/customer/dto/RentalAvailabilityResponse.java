package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentalAvailabilityResponse {
    private Integer equipmentId;
    private String equipmentName;
    private Boolean isAvailable;
    private LocalDate queriedStartDate;
    private LocalDate queriedEndDate;
    private List<UnavailableSlot> bookedSlots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnavailableSlot {
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
    }
}
