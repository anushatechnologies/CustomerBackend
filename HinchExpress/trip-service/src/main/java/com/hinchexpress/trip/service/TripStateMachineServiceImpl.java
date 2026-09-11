package com.hinchexpress.trip.service;

import com.hinchexpress.common.enums.TripStatus;
import com.hinchexpress.trip.dto.AssignDriverRequest;
import com.hinchexpress.trip.entity.Trip;
import com.hinchexpress.trip.entity.TripCheckpoint;
import com.hinchexpress.trip.exception.IllegalTripTransitionException;
import com.hinchexpress.trip.repository.TripCheckpointRepository;
import com.hinchexpress.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripStateMachineServiceImpl implements TripStateMachineService {

    private final TripRepository tripRepository;
    private final TripCheckpointRepository checkpointRepository;
    private final OtpService otpService;
    private final TripOutboxService outboxService;

    @Override
    @Transactional
    public Trip assignDriver(Trip trip, AssignDriverRequest request) {
        if (trip.getStatus() != TripStatus.DISPATCH_PENDING) {
            throw new IllegalTripTransitionException(
                    "Cannot assign driver to trip " + trip.getTripNumber() + " in status " + trip.getStatus());
        }

        trip.setDriverId(request.getDriverId());
        trip.setVehicleId(request.getVehicleId());
        trip.setVehicleNumber(request.getVehicleNumber());
        trip.setDriverName(request.getDriverName());
        trip.setDriverPhone(request.getDriverPhone());
        trip.setStatus(TripStatus.DRIVER_ASSIGNED);
        trip.setAssignedAt(LocalDateTime.now());

        Trip saved = tripRepository.save(trip);
        recordCheckpoint(saved, TripStatus.DRIVER_ASSIGNED, "Driver Assigned",
                "Driver " + request.getDriverName() + " assigned to order #" + trip.getOrderId(),
                trip.getPickupAddress(), trip.getPickupLatitude(), trip.getPickupLongitude());
        outboxService.recordTripEvent(saved, "DRIVER_ASSIGNED", "Driver assigned to trip");
        return saved;
    }

    @Override
    @Transactional
    public Trip markArrivedAtPickup(Trip trip) {
        if (trip.getStatus() != TripStatus.DRIVER_ASSIGNED) {
            throw new IllegalTripTransitionException(
                    "Cannot mark arrived at pickup for trip " + trip.getTripNumber() + " in status " + trip.getStatus());
        }

        trip.setStatus(TripStatus.ARRIVED_AT_PICKUP);
        trip.setArrivedAtPickupAt(LocalDateTime.now());

        Trip saved = tripRepository.save(trip);
        recordCheckpoint(saved, TripStatus.ARRIVED_AT_PICKUP, "Arrived at Store",
                "Driver arrived at pickup location", trip.getPickupAddress(),
                trip.getPickupLatitude(), trip.getPickupLongitude());
        outboxService.recordTripEvent(saved, "ARRIVED_AT_PICKUP", "Driver arrived at store");
        return saved;
    }

    @Override
    @Transactional
    public Trip verifyPickupOtpAndConfirmPickup(Trip trip, String otp) {
        if (trip.getStatus() != TripStatus.ARRIVED_AT_PICKUP) {
            throw new IllegalTripTransitionException(
                    "Cannot verify pickup OTP for trip " + trip.getTripNumber() + " in status " + trip.getStatus());
        }

        otpService.verifyPickupOtp(trip, otp);
        trip.setStatus(TripStatus.PICKED_UP);
        trip.setPickedUpAt(LocalDateTime.now());

        Trip saved = tripRepository.save(trip);
        recordCheckpoint(saved, TripStatus.PICKED_UP, "Order Picked Up",
                "Pickup OTP verified. Order loaded on vehicle", trip.getPickupAddress(),
                trip.getPickupLatitude(), trip.getPickupLongitude());
        outboxService.recordTripEvent(saved, "PICKED_UP", "Pickup verified and order loaded");
        return saved;
    }

    @Override
    @Transactional
    public Trip startTransit(Trip trip) {
        if (trip.getStatus() != TripStatus.PICKED_UP) {
            throw new IllegalTripTransitionException(
                    "Cannot transition to in-transit for trip " + trip.getTripNumber() + " in status " + trip.getStatus());
        }

        trip.setStatus(TripStatus.IN_TRANSIT);

        Trip saved = tripRepository.save(trip);
        recordCheckpoint(saved, TripStatus.IN_TRANSIT, "In Transit",
                "Driver departed store and is en route to customer", "En route",
                trip.getPickupLatitude(), trip.getPickupLongitude());
        outboxService.recordTripEvent(saved, "IN_TRANSIT", "Driver departed and en route");
        return saved;
    }

    @Override
    @Transactional
    public Trip markArrivedAtDelivery(Trip trip) {
        if (trip.getStatus() != TripStatus.IN_TRANSIT && trip.getStatus() != TripStatus.PICKED_UP) {
            throw new IllegalTripTransitionException(
                    "Cannot mark arrived at delivery for trip " + trip.getTripNumber() + " in status " + trip.getStatus());
        }

        trip.setStatus(TripStatus.ARRIVED_AT_DELIVERY);
        trip.setArrivedAtDeliveryAt(LocalDateTime.now());

        Trip saved = tripRepository.save(trip);
        recordCheckpoint(saved, TripStatus.ARRIVED_AT_DELIVERY, "Arrived at Customer Location",
                "Driver arrived at customer destination", trip.getDeliveryAddress(),
                trip.getDeliveryLatitude(), trip.getDeliveryLongitude());
        outboxService.recordTripEvent(saved, "ARRIVED_AT_DELIVERY", "Driver arrived at customer destination");
        return saved;
    }

    @Override
    @Transactional
    public Trip verifyDeliveryOtpAndComplete(Trip trip, String otp) {
        if (trip.getStatus() != TripStatus.ARRIVED_AT_DELIVERY) {
            throw new IllegalTripTransitionException(
                    "Cannot verify delivery OTP for trip " + trip.getTripNumber() + " in status " + trip.getStatus());
        }

        otpService.verifyDeliveryOtp(trip, otp);
        trip.setStatus(TripStatus.DELIVERED);
        trip.setDeliveredAt(LocalDateTime.now());

        Trip saved = tripRepository.save(trip);
        recordCheckpoint(saved, TripStatus.DELIVERED, "Order Delivered",
                "Delivery OTP verified. Order handed over successfully", trip.getDeliveryAddress(),
                trip.getDeliveryLatitude(), trip.getDeliveryLongitude());
        outboxService.recordTripEvent(saved, "DELIVERED", "Order successfully delivered to customer");
        return saved;
    }

    @Override
    @Transactional
    public Trip cancelTrip(Trip trip, String reason) {
        if (trip.getStatus() == TripStatus.DELIVERED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new IllegalTripTransitionException(
                    "Cannot cancel trip " + trip.getTripNumber() + " in status " + trip.getStatus());
        }

        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelledAt(LocalDateTime.now());
        trip.setCancellationReason(reason);

        Trip saved = tripRepository.save(trip);
        recordCheckpoint(saved, TripStatus.CANCELLED, "Trip Cancelled",
                "Trip cancelled: " + reason, null, null, null);
        outboxService.recordTripEvent(saved, "CANCELLED", "Trip cancelled: " + reason);
        return saved;
    }

    private void recordCheckpoint(Trip trip, TripStatus status, String title, String description,
                                  String locationName, Double lat, Double lng) {
        TripCheckpoint checkpoint = TripCheckpoint.builder()
                .tripId(trip.getId())
                .checkpointStatus(status)
                .title(title)
                .description(description)
                .locationName(locationName)
                .latitude(lat)
                .longitude(lng)
                .recordedAt(LocalDateTime.now())
                .build();
        checkpointRepository.save(checkpoint);
    }
}

