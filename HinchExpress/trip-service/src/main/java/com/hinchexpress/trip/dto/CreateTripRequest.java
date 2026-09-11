package com.hinchexpress.trip.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {

    @NotNull(message = "Order ID is required")
    private Integer orderId;

    @NotNull(message = "Customer ID is required")
    private Integer customerId;

    @NotNull(message = "Store ID is required")
    private Integer storeId;

    @NotNull(message = "Pickup latitude is required")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    private Double pickupLongitude;

    private String pickupAddress;

    private Integer deliveryAddressId;

    @NotNull(message = "Delivery latitude is required")
    private Double deliveryLatitude;

    @NotNull(message = "Delivery longitude is required")
    private Double deliveryLongitude;

    private String deliveryAddress;

    @NotNull(message = "Total weight in kg is required")
    @DecimalMin(value = "0.001", message = "Weight must be greater than 0")
    private BigDecimal totalWeightKg;
}