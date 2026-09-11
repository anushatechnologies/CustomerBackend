package com.hinchexpress.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hinchexpress.common.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outgoing event contract published by HinchExpress to Kafka topic 'delivery.events'.
 * Notifies HinchMart and external systems of delivery milestone transitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryEvent {

    private String eventId;
    private String eventType;
    private Integer orderId;
    private String tripNumber;
    private TripStatus status;
    private Long vehicleId;
    private String vehicleNumber;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private Double currentLatitude;
    private Double currentLongitude;
    private Integer etaMinutes;
    private String remarks;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
