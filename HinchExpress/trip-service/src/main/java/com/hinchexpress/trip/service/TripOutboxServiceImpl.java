package com.hinchexpress.trip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinchexpress.common.event.DeliveryEvent;
import com.hinchexpress.trip.entity.DeliveryOutboxEvent;
import com.hinchexpress.trip.entity.Trip;
import com.hinchexpress.trip.repository.DeliveryOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripOutboxServiceImpl implements TripOutboxService {

    private final DeliveryOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public DeliveryOutboxEvent recordTripEvent(Trip trip, String eventType, String remarks) {
        String eventId = UUID.randomUUID().toString();

        Double lat = trip.getPickupLatitude();
        Double lng = trip.getPickupLongitude();
        if (trip.getStatus() != null) {
            switch (trip.getStatus()) {
                case ARRIVED_AT_DELIVERY:
                case DELIVERED:
                    lat = trip.getDeliveryLatitude();
                    lng = trip.getDeliveryLongitude();
                    break;
                default:
                    break;
            }
        }

        DeliveryEvent deliveryEvent = DeliveryEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .orderId(trip.getOrderId())
                .tripNumber(trip.getTripNumber())
                .status(trip.getStatus())
                .vehicleId(trip.getVehicleId())
                .vehicleNumber(trip.getVehicleNumber())
                .driverId(trip.getDriverId())
                .driverName(trip.getDriverName())
                .driverPhone(trip.getDriverPhone())
                .currentLatitude(lat)
                .currentLongitude(lng)
                .etaMinutes(trip.getEstimatedDurationMinutes())
                .remarks(remarks)
                .timestamp(LocalDateTime.now())
                .build();

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(deliveryEvent);
        } catch (Exception e) {
            log.error("Failed to serialize DeliveryEvent for trip {}: {}", trip.getTripNumber(), e.getMessage());
            payloadJson = "{}";
        }

        DeliveryOutboxEvent outboxEvent = DeliveryOutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("TRIP")
                .aggregateId(String.valueOf(trip.getId()))
                .eventType(eventType)
                .payload(payloadJson)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        DeliveryOutboxEvent saved = outboxEventRepository.save(outboxEvent);
        log.info("Recorded transactional outbox event [{}] for trip: {}", eventType, trip.getTripNumber());
        return saved;
    }
}

