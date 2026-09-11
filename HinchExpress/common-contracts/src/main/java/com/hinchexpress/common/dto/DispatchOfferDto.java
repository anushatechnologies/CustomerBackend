package com.hinchexpress.common.dto;

import com.hinchexpress.common.enums.DispatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dispatch delivery request offered to a single driver with a 15-second response window.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchOfferDto {

    private Long attemptId;
    private Long tripId;
    private String tripNumber;
    private Integer orderId;
    private Long driverId;
    private Long vehicleId;
    private String vehicleNumber;
    private int attemptNumber;
    private DispatchStatus status;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String deliveryAddress;
    private BigDecimal totalWeightKg;
    private BigDecimal estimatedDistanceKm;
    private LocalDateTime sentAt;
    private LocalDateTime expiresAt;
    private long secondsRemaining;
}
