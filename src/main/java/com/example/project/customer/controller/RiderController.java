package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.DeliveryVerificationRequest;
import com.example.project.customer.dto.DeliveryVerificationResponse;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.RiderLocationUpdateRequest;
import com.example.project.customer.dto.RiderOfferRespondRequest;
import com.example.project.customer.dto.RiderOfferResponse;
import com.example.project.customer.entity.DeliveryRider;
import com.example.project.customer.service.DeliveryVerificationService;
import com.example.project.customer.service.RiderDispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/rider")
@RequiredArgsConstructor
public class RiderController {

    private final RiderDispatchService riderDispatchService;
    private final DeliveryVerificationService deliveryVerificationService;

    @PostMapping("/status")
    public ResponseEntity<ApiResponse<DeliveryRider>> updateRiderStatus(
            @RequestParam(required = false) Long riderId,
            @RequestHeader(value = "X-Rider-Id", required = false) Long headerRiderId,
            @RequestBody RiderLocationUpdateRequest request
    ) {
        Long effectiveRiderId = resolveRiderId(riderId, headerRiderId);
        DeliveryRider updated = riderDispatchService.updateRiderStatus(effectiveRiderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Rider status and telemetry updated successfully", updated));
    }

    @GetMapping("/offers/active")
    public ResponseEntity<ApiResponse<RiderOfferResponse>> getActiveOffer(
            @RequestParam(required = false) Long riderId,
            @RequestHeader(value = "X-Rider-Id", required = false) Long headerRiderId
    ) {
        Long effectiveRiderId = resolveRiderId(riderId, headerRiderId);
        Optional<RiderOfferResponse> activeOffer = riderDispatchService.getActiveOfferForRider(effectiveRiderId);

        if (activeOffer.isPresent()) {
            return ResponseEntity.ok(ApiResponse.ok("Active dispatch offer retrieved", activeOffer.get()));
        } else {
            return ResponseEntity.ok(ApiResponse.ok("No active dispatch offers at this time", null));
        }
    }

    @GetMapping("/orders/active")
    public ResponseEntity<ApiResponse<OrderResponse>> getActiveOrder(
            @RequestParam(required = false) Long riderId,
            @RequestHeader(value = "X-Rider-Id", required = false) Long headerRiderId
    ) {
        Long effectiveRiderId = resolveRiderId(riderId, headerRiderId);
        Optional<OrderResponse> activeOrder = riderDispatchService.getActiveAssignedOrder(effectiveRiderId);

        if (activeOrder.isPresent()) {
            return ResponseEntity.ok(ApiResponse.ok("Active assigned delivery order retrieved", activeOrder.get()));
        } else {
            return ResponseEntity.ok(ApiResponse.ok("No active delivery order assigned at this time", null));
        }
    }

    @PostMapping("/offers/{offerId}/respond")
    public ResponseEntity<ApiResponse<Boolean>> respondToOffer(
            @PathVariable Long offerId,
            @RequestParam(required = false) Long riderId,
            @RequestHeader(value = "X-Rider-Id", required = false) Long headerRiderId,
            @Valid @RequestBody RiderOfferRespondRequest request
    ) {
        Long effectiveRiderId = resolveRiderId(riderId, headerRiderId);
        boolean success = riderDispatchService.respondToOffer(effectiveRiderId, offerId, request);
        String msg = success ? "Offer response processed successfully" : "Offer could not be accepted (expired or assigned)";
        return ResponseEntity.ok(ApiResponse.ok(msg, success));
    }

    @PostMapping("/orders/{orderId}/pickup")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmPickup(
            @PathVariable Integer orderId,
            @RequestParam(required = false) Long riderId,
            @RequestHeader(value = "X-Rider-Id", required = false) Long headerRiderId
    ) {
        Long effectiveRiderId = resolveRiderId(riderId, headerRiderId);
        OrderResponse response = deliveryVerificationService.confirmPickup(effectiveRiderId, orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order picked up successfully and marked in transit", response));
    }

    @PostMapping("/orders/{orderId}/arrived")
    public ResponseEntity<ApiResponse<OrderResponse>> markArrivedAtCustomer(
            @PathVariable Integer orderId,
            @RequestParam(required = false) Long riderId,
            @RequestHeader(value = "X-Rider-Id", required = false) Long headerRiderId
    ) {
        Long effectiveRiderId = resolveRiderId(riderId, headerRiderId);
        OrderResponse response = deliveryVerificationService.markArrivedAtCustomer(effectiveRiderId, orderId);
        return ResponseEntity.ok(ApiResponse.ok("Rider marked arrived at customer delivery site", response));
    }

    @PostMapping("/orders/{orderId}/verify-delivery")
    public ResponseEntity<ApiResponse<DeliveryVerificationResponse>> verifyAndCompleteDelivery(
            @PathVariable Integer orderId,
            @RequestParam(required = false) Long riderId,
            @RequestHeader(value = "X-Rider-Id", required = false) Long headerRiderId,
            @Valid @RequestBody DeliveryVerificationRequest request
    ) {
        Long effectiveRiderId = resolveRiderId(riderId, headerRiderId);
        DeliveryVerificationResponse result = deliveryVerificationService.verifyAndCompleteDelivery(
                effectiveRiderId, orderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Delivery completed and verified successfully", result));
    }

    private Long resolveRiderId(Long paramRiderId, Long headerRiderId) {
        if (paramRiderId != null) return paramRiderId;
        if (headerRiderId != null) return headerRiderId;
        return 1L; // Default test rider ID fallback
    }
}
