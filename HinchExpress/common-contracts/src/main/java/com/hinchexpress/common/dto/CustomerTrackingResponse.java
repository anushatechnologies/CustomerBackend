package com.hinchexpress.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hinchexpress.common.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerTrackingResponse {

    private Integer orderId;
    private String tripNumber;
    private TripStatus status;
    private String statusDescription;

    // Vehicle & Driver
    private String vehicleTier;
    private String vehicleModel;
    private String vehicleNumber;
    private String driverName;
    private String driverPhone;
    private BigDecimal driverRating;

    // Route coordinates
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String deliveryAddress;

    // Real-time telemetry
    private Double currentLatitude;
    private Double currentLongitude;
    private BigDecimal remainingDistanceKm;
    private Integer etaMinutes;
    private LocalDateTime lastPingAt;

    // Milestone Checkpoints Timeline
    private List<CheckpointItem> checkpoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckpointItem {
        private String status;
        private String title;
        private String description;
        private String locationName;
        private Double latitude;
        private Double longitude;
        private LocalDateTime timestamp;
    }
}
