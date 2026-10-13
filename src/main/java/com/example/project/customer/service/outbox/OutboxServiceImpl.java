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
public class OutboxServiceImpl implements OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final AddressRepository addressRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;
    private final com.example.project.customer.config.PlatformMarketplaceProperties platformMarketplaceProperties;

    @org.springframework.beans.factory.annotation.Autowired
    public OutboxServiceImpl(
            OutboxEventRepository outboxEventRepository,
            AddressRepository addressRepository,
            StoreRepository storeRepository,
            ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.example.project.customer.config.PlatformMarketplaceProperties platformMarketplaceProperties
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.addressRepository = addressRepository;
        this.storeRepository = storeRepository;
        this.objectMapper = objectMapper;
        this.platformMarketplaceProperties = platformMarketplaceProperties;
    }

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
        boolean isHinchOrder = (order.getStore() != null && platformMarketplaceProperties != null
                && platformMarketplaceProperties.isPlatformStore(order.getStore().getStoreId()));

        Store pickupStore;
        if (isHinchOrder) {
            pickupStore = order.getFulfillmentStore();
            if (pickupStore == null) {
                throw new IllegalStateException("HinchStore order #" + order.getOrderId() + " has no fulfillment store configured.");
            }
        } else {
            pickupStore = order.getFulfillmentStore() != null ? order.getFulfillmentStore() : order.getStore();
            if (pickupStore == null && order.getOrderId() != null) {
                // Lazy load safeguard if detached
                pickupStore = storeRepository.findAll().stream().findFirst().orElse(null);
            }
        }

        Double pickupLat = pickupStore != null ? pickupStore.getLatitude() : null;
        Double pickupLng = pickupStore != null ? pickupStore.getLongitude() : null;

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
                .storeId(order.getStore() != null ? order.getStore().getStoreId() : (pickupStore != null ? pickupStore.getStoreId() : null))
                .pickupLatitude(pickupLat)
                .pickupLongitude(pickupLng)
                .deliveryAddressId(order.getAddressId())
                .deliveryLatitude(deliveryLat)
                .deliveryLongitude(deliveryLng)
                .totalWeightKg(order.getTotalWeightKg())
                .fulfillmentStoreId(order.getFulfillmentStore() != null ? order.getFulfillmentStore().getStoreId() : null)
                .fulfillmentStoreName(order.getFulfillmentStore() != null ? order.getFulfillmentStore().getName() : null)
                .pickupAddress(pickupStore != null ? pickupStore.getName() : null)
                .eventVersion("2.0")
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
