package com.example.project.customer.service;

import com.example.project.customer.dto.DeliveryVerificationRequest;
import com.example.project.customer.dto.DeliveryVerificationResponse;
import com.example.project.customer.dto.OrderResponse;

public interface DeliveryVerificationService {

    OrderResponse confirmPickup(Long riderId, Integer orderId);

    OrderResponse markArrivedAtCustomer(Long riderId, Integer orderId);

    DeliveryVerificationResponse verifyAndCompleteDelivery(Long riderId, Integer orderId, DeliveryVerificationRequest request);
}
