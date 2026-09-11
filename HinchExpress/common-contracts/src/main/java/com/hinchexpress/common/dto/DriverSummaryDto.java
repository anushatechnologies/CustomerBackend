package com.hinchexpress.common.dto;

import com.hinchexpress.common.enums.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverSummaryDto {

    private Long id;
    private String name;
    private String phone;
    private String licenseNumber;
    private DriverStatus status;
    private BigDecimal rating;
    private int totalTripsCompleted;
    private boolean active;
}
