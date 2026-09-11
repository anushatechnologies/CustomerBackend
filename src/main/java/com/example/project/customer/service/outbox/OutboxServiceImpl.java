package com.example.project.customer.service.outbox;

import com.example.project.customer.dto.event.OrderConfirmedEvent;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OutboxEvent;
import com.example.project.customer.entity.Store;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.OutboxEventRepository;
import com.example.project.customer.repository.StoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final AddressRepository addressRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OutboxEvent recordOrderConfirmed(Order order) {
        if (order == null || order.getOrderId() == null) {
            log.warn("Cannot record OrderConfirmed outbox event: order or orderId is null");
            return null;
        }

        String orderIdStr = String.valueOf(order.getOrderId());

        // 1. Application-level idempotency guard
        if (outboxEventRepository.existsByAggregateTypeAndAggregateIdAndEventType("ORDER", orderIdStr, "ORDER_CONFIRMED")) {
            log.info("OrderConfirmed outbox event already exists for order #{}. Skipping duplicate creation.", order.getOrderId());
            return null;
        }

        // 2. Resolve pickup location from Store
        Store store = order.getStore();
        if (store == null && order.getOrderId() != null) {
            // Lazy load safeguard if detached
            store = storeRepository.findById(1).orElse(null);
        }
        Double pickupLat = store != null ? store.getLatitude() : null;
        Double pickupLng = store != null ? store.getLongitude() : null;

        // 3. Resolve delivery location from Address
        Double deliveryLat = null;
        Double deliveryLng = null;
        if (order.getAddressId() != null) {
            Address address = addressRepository.findById(order.getAddressId()).orElse(null);
            if (address != null) {
                deliveryLat = address.getLatitude();
                deliveryLng = address.getLongitude();
            }
        }

        // 4. Build OrderConfirmed event DTO
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomer() != null ? order.getCustomer().getCustomerId() : null)
                .storeId(store != null ? store.getStoreId() : null)
                .pickupLatitude(pickupLat)
                .pickupLongitude(pickupLng)
                .deliveryAddressId(order.getAddressId())
                .deliveryLatitude(deliveryLat)
                .deliveryLongitude(deliveryLng)
                .totalWeightKg(order.getTotalWeightKg())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(orderIdStr)
                    .eventType("ORDER_CONFIRMED")
                    .payload(payload)
                    .status("PENDING")
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            OutboxEvent saved = outboxEventRepository.save(outboxEvent);
            log.info("Created transactional OutboxEvent #{} for Order #{}", saved.getId(), order.getOrderId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Database-level race condition guard (unique key violation)
            log.warn("Database unique constraint prevented duplicate OutboxEvent for Order #{}: {}", order.getOrderId(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Failed to serialize or save OutboxEvent for Order #{}: {}", order.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to persist outbox event: " + e.getMessage(), e);
        }
    }
}
