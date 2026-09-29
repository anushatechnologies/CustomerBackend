package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiderOfferResponse {

    private Long offerId;
    private Integer orderId;
    private String orderNumber;
    private Integer sequenceIndex;
    private Double distanceKm;
    private BigDecimal offeredFare;
    private String status;
    private LocalDateTime offeredAt;
    private LocalDateTime expiresAt;
    private Long remainingSeconds;

    private String storeName;
    private Double storeLatitude;
    private Double storeLongitude;
    private String deliveryLocation;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String pickupNavigationUrl;
    private String deliveryNavigationUrl;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private Integer itemCount;
}
