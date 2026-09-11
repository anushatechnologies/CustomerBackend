package com.hinchexpress.trip.controller;

import com.hinchexpress.common.dto.ApiResponse;
import com.hinchexpress.common.dto.TripSummaryDto;
import com.hinchexpress.trip.dto.AssignDriverRequest;
import com.hinchexpress.trip.dto.CheckpointDto;
import com.hinchexpress.trip.dto.CreateTripRequest;
import com.hinchexpress.trip.dto.VerifyOtpRequest;
import com.hinchexpress.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<ApiResponse<TripSummaryDto>> createTrip(@Valid @RequestBody CreateTripRequest request) {
        TripSummaryDto trip = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Delivery trip created successfully", trip));
    }

    @PostMapping("/{tripId}/assign-driver")
    public ResponseEntity<ApiResponse<TripSummaryDto>> assignDriver(
            @PathVariable Long tripId,
            @Valid @RequestBody AssignDriverRequest request) {
        TripSummaryDto trip = tripService.assignDriver(tripId, request);
        return ResponseEntity.ok(ApiResponse.ok("Driver assigned to trip", trip));
    }

    @PostMapping("/{tripId}/arrived-pickup")
    public ResponseEntity<ApiResponse<TripSummaryDto>> markArrivedAtPickup(@PathVariable Long tripId) {
        TripSummaryDto trip = tripService.markArrivedAtPickup(tripId);
        return ResponseEntity.ok(ApiResponse.ok("Driver marked as arrived at pickup", trip));
    }

    @PostMapping("/{tripId}/verify-pickup-otp")
    public ResponseEntity<ApiResponse<TripSummaryDto>> verifyPickupOtp(
            @PathVariable Long tripId,
            @Valid @RequestBody VerifyOtpRequest request) {
        TripSummaryDto trip = tripService.verifyPickupOtp(tripId, request.getOtp());
        return ResponseEntity.ok(ApiResponse.ok("Pickup OTP verified successfully. Order is picked up.", trip));
    }

    @PostMapping("/{tripId}/start-transit")
    public ResponseEntity<ApiResponse<TripSummaryDto>> startTransit(@PathVariable Long tripId) {
        TripSummaryDto trip = tripService.startTransit(tripId);
        return ResponseEntity.ok(ApiResponse.ok("Trip transitioned to in-transit", trip));
    }

    @PostMapping("/{tripId}/arrived-delivery")
    public ResponseEntity<ApiResponse<TripSummaryDto>> markArrivedAtDelivery(@PathVariable Long tripId) {
        TripSummaryDto trip = tripService.markArrivedAtDelivery(tripId);
        return ResponseEntity.ok(ApiResponse.ok("Driver arrived at customer delivery location", trip));
    }

    @PostMapping("/{tripId}/verify-delivery-otp")
    public ResponseEntity<ApiResponse<TripSummaryDto>> verifyDeliveryOtp(
            @PathVariable Long tripId,
            @Valid @RequestBody VerifyOtpRequest request) {
        TripSummaryDto trip = tripService.verifyDeliveryOtp(tripId, request.getOtp());
        return ResponseEntity.ok(ApiResponse.ok("Delivery OTP verified successfully. Order delivered.", trip));
    }

    @PostMapping("/{tripId}/cancel")
    public ResponseEntity<ApiResponse<TripSummaryDto>> cancelTrip(
            @PathVariable Long tripId,
            @RequestParam(required = false, defaultValue = "Cancelled by user/system") String reason) {
        TripSummaryDto trip = tripService.cancelTrip(tripId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Trip cancelled successfully", trip));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripSummaryDto>> getTripById(@PathVariable Long tripId) {
        TripSummaryDto trip = tripService.getTripById(tripId);
        return ResponseEntity.ok(ApiResponse.ok("Trip retrieved successfully", trip));
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<ApiResponse<TripSummaryDto>> getTripByOrderId(@PathVariable Integer orderId) {
        TripSummaryDto trip = tripService.getTripByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Trip retrieved for order #" + orderId, trip));
    }

    @GetMapping("/{tripId}/checkpoints")
    public ResponseEntity<ApiResponse<List<CheckpointDto>>> getTripCheckpoints(@PathVariable Long tripId) {
        List<CheckpointDto> checkpoints = tripService.getTripCheckpoints(tripId);
        return ResponseEntity.ok(ApiResponse.ok("Checkpoints timeline retrieved", checkpoints));
    }
}

