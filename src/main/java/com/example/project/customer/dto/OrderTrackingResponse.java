package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderTrackingResponse {
    private Integer orderId;
    private String orderNumber;
    private String carrierName;
    private String vehicleNumber;
    private String driverName;
    private String trackingNumber;
    private String currentStatus;
    private LocalDateTime estimatedDelivery;

    @Builder.Default
    private List<TrackingCheckpointDto> checkpoints = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingCheckpointDto {
        private String status;
        private String title;
        private String location;
        private LocalDateTime timestamp;
        private String description;
    }
}
