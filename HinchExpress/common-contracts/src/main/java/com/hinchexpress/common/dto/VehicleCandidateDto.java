package com.hinchexpress.common.dto;

import com.hinchexpress.common.enums.VehicleTierCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Candidate vehicle evaluated by the fleet matching engine for dispatch eligibility.
 * Shared across fleet-service (provider) and dispatch-service (consumer).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCandidateDto {

    private Long vehicleId;
    private String vehicleNumber;
    private VehicleTierCode tierCode;
    private String tierName;
    private BigDecimal maxPayloadKg;
    private String modelName;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private BigDecimal driverRating;
    private Double currentLatitude;
    private Double currentLongitude;
    private Double distanceKmToPickup;
}