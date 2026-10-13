package com.example.project.customer.dto.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderConfirmedEvent {

    private Integer orderId;
    private Integer customerId;
    private Integer storeId;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Integer deliveryAddressId;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private BigDecimal totalWeightKg;

    // Additive version 2.0 fields
    private Integer fulfillmentStoreId;
    private String fulfillmentStoreName;
    private String pickupAddress;
    @Builder.Default
    private String eventVersion = "2.0";
}
