package com.hinchexpress.trip;

import com.hinchexpress.common.dto.TripSummaryDto;
import com.hinchexpress.common.enums.TripStatus;
import com.hinchexpress.trip.dto.AssignDriverRequest;
import com.hinchexpress.trip.dto.CheckpointDto;
import com.hinchexpress.trip.dto.CreateTripRequest;
import com.hinchexpress.trip.entity.DeliveryOutboxEvent;
import com.hinchexpress.trip.exception.IllegalTripTransitionException;
import com.hinchexpress.trip.exception.InvalidOtpException;
import com.hinchexpress.trip.repository.DeliveryOutboxEventRepository;
import com.hinchexpress.trip.repository.TripRepository;
import com.hinchexpress.trip.service.TripService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TripServiceApplicationTests {

    @Autowired
    private TripService tripService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private DeliveryOutboxEventRepository outboxEventRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private CreateTripRequest createSampleRequest(Integer orderId) {
        return CreateTripRequest.builder()
                .orderId(orderId)
                .customerId(101)
                .storeId(5)
                .pickupLatitude(17.385044)
                .pickupLongitude(78.486671)
                .pickupAddress("HinchMart Store #5, MG Road")
                .deliveryAddressId(202)
                .deliveryLatitude(17.440081)
                .deliveryLongitude(78.348915)
                .deliveryAddress("Flat 402, Sunshine Heights, Gachibowli")
                .totalWeightKg(new BigDecimal("450.000"))
                .build();
    }

    @Test
    @DisplayName("1. Context loads cleanly")
    void contextLoads() {
        assertThat(tripService).isNotNull();
    }

    @Test
    @DisplayName("2. Enforces One Order = One Trip invariant idempotently")
    void testOneOrderOneTripInvariant() {
        CreateTripRequest req = createSampleRequest(9001);
        TripSummaryDto trip1 = tripService.createTrip(req);
        assertThat(trip1).isNotNull();
        assertThat(trip1.getOrderId()).isEqualTo(9001);
        assertThat(trip1.getStatus()).isEqualTo(TripStatus.DISPATCH_PENDING);
        assertThat(trip1.getPickupOtp()).hasSize(6);
        assertThat(trip1.getDeliveryOtp()).hasSize(6);

        // Calling createTrip again with the same orderId must idempotently return the existing trip
        TripSummaryDto trip2 = tripService.createTrip(req);
        assertThat(trip2.getTripId()).isEqualTo(trip1.getTripId());
        assertThat(trip2.getTripNumber()).isEqualTo(trip1.getTripNumber());

        // Ensure database table has exactly one record for this order
        assertThat(tripRepository.findByOrderId(9001)).isPresent();
    }

    @Test
    @DisplayName("3. Happy Path: Full state machine lifecycle with dual OTP verification")
    void testFullTripLifecycleHappyPath() {
        TripSummaryDto trip = tripService.createTrip(createSampleRequest(9002));
        Long tripId = trip.getTripId();
        String pickupOtp = trip.getPickupOtp();
        String deliveryOtp = trip.getDeliveryOtp();

        // 1. Assign Driver
        AssignDriverRequest assignReq = AssignDriverRequest.builder()
                .driverId(1L)
                .vehicleId(10L)
                .vehicleNumber("KA-01-EQ-9090")
                .driverName("Ramesh Kumar")
                .driverPhone("+919876543210")
                .build();
        TripSummaryDto assigned = tripService.assignDriver(tripId, assignReq);
        assertThat(assigned.getStatus()).isEqualTo(TripStatus.DRIVER_ASSIGNED);
        assertThat(assigned.getDriverName()).isEqualTo("Ramesh Kumar");

        // 2. Driver arrives at store
        TripSummaryDto atPickup = tripService.markArrivedAtPickup(tripId);
        assertThat(atPickup.getStatus()).isEqualTo(TripStatus.ARRIVED_AT_PICKUP);

        // 3. Store verifies Pickup OTP -> Picked up
        TripSummaryDto pickedUp = tripService.verifyPickupOtp(tripId, pickupOtp);
        assertThat(pickedUp.getStatus()).isEqualTo(TripStatus.PICKED_UP);

        // 4. Start transit to customer
        TripSummaryDto inTransit = tripService.startTransit(tripId);
        assertThat(inTransit.getStatus()).isEqualTo(TripStatus.IN_TRANSIT);

        // 5. Driver arrives at customer destination
        TripSummaryDto atDelivery = tripService.markArrivedAtDelivery(tripId);
        assertThat(atDelivery.getStatus()).isEqualTo(TripStatus.ARRIVED_AT_DELIVERY);

        // 6. Customer verifies Delivery OTP -> Delivered
        TripSummaryDto delivered = tripService.verifyDeliveryOtp(tripId, deliveryOtp);
        assertThat(delivered.getStatus()).isEqualTo(TripStatus.DELIVERED);
        assertThat(delivered.getDeliveredAt()).isNotNull();

        // Verify Checkpoints timeline
        List<CheckpointDto> checkpoints = tripService.getTripCheckpoints(tripId);
        assertThat(checkpoints).hasSize(7);
        assertThat(checkpoints.get(0).getCheckpointStatus()).isEqualTo(TripStatus.DISPATCH_PENDING);
        assertThat(checkpoints.get(6).getCheckpointStatus()).isEqualTo(TripStatus.DELIVERED);
    }

    @Test
    @DisplayName("4. Pickup OTP: Invalid code is rejected with InvalidOtpException")
    void testPickupOtpRejection() {
        TripSummaryDto trip = tripService.createTrip(createSampleRequest(9003));
        tripService.assignDriver(trip.getTripId(), AssignDriverRequest.builder()
                .driverId(2L).vehicleId(11L).vehicleNumber("KA-01-AB-1111")
                .driverName("Suresh").driverPhone("+919111122223").build());
        tripService.markArrivedAtPickup(trip.getTripId());

        assertThatThrownBy(() -> tripService.verifyPickupOtp(trip.getTripId(), "000000"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid pickup OTP");
    }

    @Test
    @DisplayName("5. Delivery OTP: Invalid code is rejected with InvalidOtpException")
    void testDeliveryOtpRejection() {
        TripSummaryDto trip = tripService.createTrip(createSampleRequest(9004));
        tripService.assignDriver(trip.getTripId(), AssignDriverRequest.builder()
                .driverId(3L).vehicleId(12L).vehicleNumber("KA-01-CD-2222")
                .driverName("Mahesh").driverPhone("+919222233334").build());
        tripService.markArrivedAtPickup(trip.getTripId());
        tripService.verifyPickupOtp(trip.getTripId(), trip.getPickupOtp());
        tripService.startTransit(trip.getTripId());
        tripService.markArrivedAtDelivery(trip.getTripId());

        assertThatThrownBy(() -> tripService.verifyDeliveryOtp(trip.getTripId(), "999999"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid delivery OTP");
    }

    @Test
    @DisplayName("6. Illegal state transition is rejected")
    void testIllegalStateTransition() {
        TripSummaryDto trip = tripService.createTrip(createSampleRequest(9005));
        // Trip is in DISPATCH_PENDING; attempting to deliver directly must fail
        assertThatThrownBy(() -> tripService.verifyDeliveryOtp(trip.getTripId(), trip.getDeliveryOtp()))
                .isInstanceOf(IllegalTripTransitionException.class)
                .hasMessageContaining("Cannot verify delivery OTP");
    }

    @Test
    @DisplayName("7. Outbox events are registered for state transitions")
    void testOutboxEventsRegistered() {
        TripSummaryDto trip = tripService.createTrip(createSampleRequest(9006));
        List<DeliveryOutboxEvent> events = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        assertThat(events).isNotEmpty();
        assertThat(events.stream().anyMatch(e -> e.getAggregateId().equals(String.valueOf(trip.getTripId())) && e.getEventType().equals("DELIVERY_CREATED"))).isTrue();
    }
}
