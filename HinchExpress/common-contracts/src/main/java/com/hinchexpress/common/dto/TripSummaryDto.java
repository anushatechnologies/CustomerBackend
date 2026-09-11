package com.hinchexpress.common.dto;

import com.hinchexpress.common.enums.TripStatus;
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
public class TripSummaryDto {

    private Long tripId;
    private String tripNumber;
    private Integer orderId;
    private Integer customerId;
    private Integer storeId;
    private TripStatus status;
    private Long vehicleId;
    private String vehicleNumber;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private Integer deliveryAddressId;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String deliveryAddress;
    private BigDecimal totalWeightKg;
    private String pickupOtp;
    private String deliveryOtp;
    private BigDecimal distanceKm;
    private Integer etaMinutes;
    private int attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
}
