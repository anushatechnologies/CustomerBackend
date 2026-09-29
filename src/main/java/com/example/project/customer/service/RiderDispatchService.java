package com.example.project.customer.service;

import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.RiderLocationUpdateRequest;
import com.example.project.customer.dto.RiderOfferRespondRequest;
import com.example.project.customer.dto.RiderOfferResponse;
import com.example.project.customer.entity.DeliveryRider;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RiderDispatchService {

    CompletableFuture<Boolean> startSequentialDispatch(Integer orderId);

    boolean offerNextRider(Integer orderId, int sequenceIndex);

    boolean respondToOffer(Long riderId, Long offerId, RiderOfferRespondRequest request);

    Optional<RiderOfferResponse> getActiveOfferForRider(Long riderId);

    Optional<OrderResponse> getActiveAssignedOrder(Long riderId);

    DeliveryRider updateRiderStatus(Long riderId, RiderLocationUpdateRequest request);

    double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2);
}
