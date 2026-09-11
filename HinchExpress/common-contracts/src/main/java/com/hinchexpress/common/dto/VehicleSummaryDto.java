package com.hinchexpress.common.dto;

import com.hinchexpress.common.enums.VehicleStatus;
import com.hinchexpress.common.enums.VehicleTierCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSummaryDto {

    private Long id;
    private String vehicleNumber;
    private VehicleTierCode tierCode;
    private String tierName;
    private String modelName;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private VehicleStatus status;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime lastPingAt;
    private boolean active;
}
