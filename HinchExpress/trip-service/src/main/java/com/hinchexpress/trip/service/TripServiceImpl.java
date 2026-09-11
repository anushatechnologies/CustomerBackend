package com.hinchexpress.trip.service;

import com.hinchexpress.common.dto.TripSummaryDto;
import com.hinchexpress.common.enums.TripStatus;
import com.hinchexpress.trip.dto.AssignDriverRequest;
import com.hinchexpress.trip.dto.CheckpointDto;
import com.hinchexpress.trip.dto.CreateTripRequest;
import com.hinchexpress.trip.entity.Trip;
import com.hinchexpress.trip.entity.TripCheckpoint;
import com.hinchexpress.trip.exception.TripNotFoundException;
import com.hinchexpress.trip.repository.TripCheckpointRepository;
import com.hinchexpress.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final TripCheckpointRepository checkpointRepository;
    private final TripStateMachineService stateMachineService;
    private final OtpService otpService;
    private final TripOutboxService outboxService;

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    @Transactional
    public TripSummaryDto createTrip(CreateTripRequest request) {
        // Core Business Invariant: ONE ORDER = ONE TRIP
        Optional<Trip> existing = tripRepository.findByOrderId(request.getOrderId());
        if (existing.isPresent()) {
            log.info("One Order = One Trip invariant: trip already exists for order #{} (tripId: {}). Idempotently returning.",
                    request.getOrderId(), existing.get().getId());
            return mapToSummaryDto(existing.get());
        }

        String tripNumber = generateTripNumber();
        String pickupOtp = otpService.generateOtp();
        String deliveryOtp = otpService.generateOtp();

        double dist = calculateHaversineDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDeliveryLatitude(), request.getDeliveryLongitude()
        );
        BigDecimal estimatedDistanceKm = BigDecimal.valueOf(dist).setScale(2, RoundingMode.HALF_UP);
        // Approximation: ~25 km/h urban speed -> ~2.4 mins per km + 10 mins handling buffer
        int estimatedMinutes = (int) Math.max(15, Math.round(dist * 2.4 + 10));

        Trip trip = Trip.builder()
                .tripNumber(tripNumber)
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .storeId(request.getStoreId())
                .status(TripStatus.DISPATCH_PENDING)
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .pickupAddress(request.getPickupAddress())
                .deliveryAddressId(request.getDeliveryAddressId())
                .deliveryLatitude(request.getDeliveryLatitude())
                .deliveryLongitude(request.getDeliveryLongitude())
                .deliveryAddress(request.getDeliveryAddress())
                .totalWeightKg(request.getTotalWeightKg())
                .estimatedDistanceKm(estimatedDistanceKm)
                .estimatedDurationMinutes(estimatedMinutes)
                .pickupOtp(pickupOtp)
                .deliveryOtp(deliveryOtp)
                .pickupOtpVerified(false)
                .deliveryOtpVerified(false)
                .otpFailedAttempts(0)
                .build();

        Trip saved = tripRepository.save(trip);

        // Record initial checkpoint and outbox event
        TripCheckpoint initialCheckpoint = TripCheckpoint.builder()
                .tripId(saved.getId())
                .checkpointStatus(TripStatus.DISPATCH_PENDING)
                .title("Delivery Requested")
                .description("Delivery requested for order #" + request.getOrderId() + " (" + request.getTotalWeightKg() + " kg)")
                .locationName(request.getPickupAddress())
                .latitude(request.getPickupLatitude())
                .longitude(request.getPickupLongitude())
                .recordedAt(LocalDateTime.now())
                .build();
        checkpointRepository.save(initialCheckpoint);

        outboxService.recordTripEvent(saved, "DELIVERY_CREATED", "Delivery trip created and pending dispatch");

        log.info("Created delivery trip {} for order #{} (weight: {} kg, estimated dist: {} km)",
                saved.getTripNumber(), saved.getOrderId(), saved.getTotalWeightKg(), estimatedDistanceKm);

        return mapToSummaryDto(saved);
    }

    @Override
    @Transactional
    public TripSummaryDto assignDriver(Long tripId, AssignDriverRequest request) {
        Trip trip = getTripEntity(tripId);
        Trip updated = stateMachineService.assignDriver(trip, request);
        return mapToSummaryDto(updated);
    }

    @Override
    @Transactional
    public TripSummaryDto markArrivedAtPickup(Long tripId) {
        Trip trip = getTripEntity(tripId);
        Trip updated = stateMachineService.markArrivedAtPickup(trip);
        return mapToSummaryDto(updated);
    }

    @Override
    @Transactional
    public TripSummaryDto verifyPickupOtp(Long tripId, String otp) {
        Trip trip = getTripEntity(tripId);
        Trip updated = stateMachineService.verifyPickupOtpAndConfirmPickup(trip, otp);
        return mapToSummaryDto(updated);
    }

    @Override
    @Transactional
    public TripSummaryDto startTransit(Long tripId) {
        Trip trip = getTripEntity(tripId);
        Trip updated = stateMachineService.startTransit(trip);
        return mapToSummaryDto(updated);
    }

    @Override
    @Transactional
    public TripSummaryDto markArrivedAtDelivery(Long tripId) {
        Trip trip = getTripEntity(tripId);
        Trip updated = stateMachineService.markArrivedAtDelivery(trip);
        return mapToSummaryDto(updated);
    }

    @Override
    @Transactional
    public TripSummaryDto verifyDeliveryOtp(Long tripId, String otp) {
        Trip trip = getTripEntity(tripId);
        Trip updated = stateMachineService.verifyDeliveryOtpAndComplete(trip, otp);
        return mapToSummaryDto(updated);
    }

    @Override
    @Transactional
    public TripSummaryDto cancelTrip(Long tripId, String reason) {
        Trip trip = getTripEntity(tripId);
        Trip updated = stateMachineService.cancelTrip(trip, reason);
        return mapToSummaryDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public TripSummaryDto getTripById(Long tripId) {
        return mapToSummaryDto(getTripEntity(tripId));
    }

    @Override
    @Transactional(readOnly = true)
    public TripSummaryDto getTripByOrderId(Integer orderId) {
        Trip trip = tripRepository.findByOrderId(orderId)
                .orElseThrow(() -> new TripNotFoundException("No trip found for order #" + orderId));
        return mapToSummaryDto(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckpointDto> getTripCheckpoints(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new TripNotFoundException("Trip with ID " + tripId + " not found");
        }
        return checkpointRepository.findByTripIdOrderByRecordedAtAsc(tripId).stream()
                .map(cp -> CheckpointDto.builder()
                        .id(cp.getId())
                        .tripId(cp.getTripId())
                        .checkpointStatus(cp.getCheckpointStatus())
                        .title(cp.getTitle())
                        .description(cp.getDescription())
                        .locationName(cp.getLocationName())
                        .latitude(cp.getLatitude())
                        .longitude(cp.getLongitude())
                        .recordedAt(cp.getRecordedAt())
                        .build())
                .toList();
    }

    private Trip getTripEntity(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trip with ID " + tripId + " not found"));
    }

    private TripSummaryDto mapToSummaryDto(Trip trip) {
        return TripSummaryDto.builder()
                .tripId(trip.getId())
                .tripNumber(trip.getTripNumber())
                .orderId(trip.getOrderId())
                .customerId(trip.getCustomerId())
                .storeId(trip.getStoreId())
                .status(trip.getStatus())
                .vehicleId(trip.getVehicleId())
                .vehicleNumber(trip.getVehicleNumber())
                .driverId(trip.getDriverId())
                .driverName(trip.getDriverName())
                .driverPhone(trip.getDriverPhone())
                .pickupLatitude(trip.getPickupLatitude())
                .pickupLongitude(trip.getPickupLongitude())
                .pickupAddress(trip.getPickupAddress())
                .deliveryAddressId(trip.getDeliveryAddressId())
                .deliveryLatitude(trip.getDeliveryLatitude())
                .deliveryLongitude(trip.getDeliveryLongitude())
                .deliveryAddress(trip.getDeliveryAddress())
                .totalWeightKg(trip.getTotalWeightKg())
                .pickupOtp(trip.getPickupOtp())
                .deliveryOtp(trip.getDeliveryOtp())
                .distanceKm(trip.getEstimatedDistanceKm())
                .etaMinutes(trip.getEstimatedDurationMinutes())
                .createdAt(trip.getCreatedAt())
                .deliveredAt(trip.getDeliveredAt())
                .build();
    }

    private String generateTripNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TRP-" + datePrefix + "-" + suffix;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }
}

