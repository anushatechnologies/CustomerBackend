package com.example.project.customer.service;

import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.RiderLocationUpdateRequest;
import com.example.project.customer.dto.RiderOfferRespondRequest;
import com.example.project.customer.dto.RiderOfferResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.DeliveryRider;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.RiderDispatchOffer;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.DeliveryRiderRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.RiderDispatchOfferRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RiderDispatchServiceImpl implements RiderDispatchService {

    private final OrderRepository orderRepository;
    private final DeliveryRiderRepository deliveryRiderRepository;
    private final RiderDispatchOfferRepository riderDispatchOfferRepository;
    private final VehicleTypeService vehicleTypeService;
    private final AddressRepository addressRepository;
    private final OrderService orderService;

    @Autowired
    public RiderDispatchServiceImpl(
            OrderRepository orderRepository,
            DeliveryRiderRepository deliveryRiderRepository,
            RiderDispatchOfferRepository riderDispatchOfferRepository,
            VehicleTypeService vehicleTypeService,
            @Autowired(required = false) AddressRepository addressRepository,
            @Autowired(required = false) OrderService orderService
    ) {
        this.orderRepository = orderRepository;
        this.deliveryRiderRepository = deliveryRiderRepository;
        this.riderDispatchOfferRepository = riderDispatchOfferRepository;
        this.vehicleTypeService = vehicleTypeService;
        this.addressRepository = addressRepository;
        this.orderService = orderService;
    }

    public RiderDispatchServiceImpl(
            OrderRepository orderRepository,
            DeliveryRiderRepository deliveryRiderRepository,
            RiderDispatchOfferRepository riderDispatchOfferRepository,
            VehicleTypeService vehicleTypeService
    ) {
        this(orderRepository, deliveryRiderRepository, riderDispatchOfferRepository, vehicleTypeService, null, null);
    }

    @Value("${app.dispatch.offer-timeout-seconds:30}")
    private int offerTimeoutSeconds = 30;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @Override
    @Async
    public CompletableFuture<Boolean> startSequentialDispatch(Integer orderId) {
        log.info("Starting sequential one-by-one rider dispatch for Order #{}", orderId);
        boolean initiated = offerNextRider(orderId, 1);
        return CompletableFuture.completedFuture(initiated);
    }

    @Override
    @Transactional
    public boolean offerNextRider(Integer orderId, int sequenceIndex) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            log.warn("Cannot dispatch: Order #{} not found", orderId);
            return false;
        }

        Order order = orderOpt.get();
        // If order already has a rider or is in a terminal state, stop dispatching
        if (order.getRiderId() != null || "DELIVERED".equalsIgnoreCase(order.getOrderStatus())
                || "CANCELLED".equalsIgnoreCase(order.getOrderStatus())
                || "REJECTED_BY_SELLER".equalsIgnoreCase(order.getOrderStatus())) {
            log.info("Order #{} already assigned or completed (status: {}). Ending dispatch waterfall.",
                    orderId, order.getOrderStatus());
            return false;
        }

        Store store = order.getStore();
        double storeLat = (store != null && store.getLatitude() != null) ? store.getLatitude() : 17.385044;
        double storeLng = (store != null && store.getLongitude() != null) ? store.getLongitude() : 78.486671;

        // Fetch already offered rider IDs for this order to exclude them from the waterfall
        List<RiderDispatchOffer> existingOffers = riderDispatchOfferRepository
                .findByOrder_OrderIdOrderBySequenceIndexAsc(orderId);

        Set<Long> alreadyOfferedRiderIds = existingOffers.stream()
                .map(o -> o.getRider().getId())
                .collect(Collectors.toSet());

        String targetVehicleType = order.getSelectedVehicleType();

        // Find candidate online riders
        List<DeliveryRider> availableRiders = deliveryRiderRepository.findAvailableOnlineRidersWithCoordinates();

        // Filter candidates:
        // 1. Not already offered for this order
        // 2. Matching seller-selected vehicle type (if specified)
        List<DeliveryRider> sortedCandidates = availableRiders.stream()
                .filter(r -> !alreadyOfferedRiderIds.contains(r.getId()))
                .filter(r -> targetVehicleType == null || targetVehicleType.isBlank()
                        || (r.getVehicleType() != null && r.getVehicleType().equalsIgnoreCase(targetVehicleType)))
                .sorted(Comparator.comparingDouble(r ->
                        calculateDistanceKm(storeLat, storeLng, r.getCurrentLatitude(), r.getCurrentLongitude())))
                .toList();

        if (sortedCandidates.isEmpty()) {
            log.warn("No available candidate riders found nearby for Order #{} (Target vehicle: '{}', total offers made: {})",
                    orderId, targetVehicleType, existingOffers.size());
            return false;
        }

        // Pick the next closest candidate rider
        DeliveryRider nextRider = sortedCandidates.get(0);
        double distanceKm = calculateDistanceKm(storeLat, storeLng, nextRider.getCurrentLatitude(), nextRider.getCurrentLongitude());
        BigDecimal fare = calculateFare(targetVehicleType, distanceKm);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(offerTimeoutSeconds);

        RiderDispatchOffer offer = RiderDispatchOffer.builder()
                .order(order)
                .rider(nextRider)
                .sequenceIndex(sequenceIndex)
                .distanceKm(distanceKm)
                .offeredFare(fare)
                .status("OFFERED")
                .offeredAt(now)
                .expiresAt(expiresAt)
                .build();

        RiderDispatchOffer savedOffer = riderDispatchOfferRepository.save(offer);

        log.info("Dispatched Offer #{} (Seq #{}) for Order #{} to Rider #{} ('{}') - Distance: {} km, Timeout: {}s",
                savedOffer.getId(), sequenceIndex, orderId, nextRider.getId(), nextRider.getName(), distanceKm, offerTimeoutSeconds);

        // Schedule non-blocking countdown timeout
        scheduleTimeout(orderId, nextRider.getId(), savedOffer.getId(), sequenceIndex, offerTimeoutSeconds);

        return true;
    }

    private void scheduleTimeout(Integer orderId, Long riderId, Long offerId, int sequenceIndex, int delaySeconds) {
        scheduler.schedule(() -> {
            try {
                handleOfferTimeout(orderId, riderId, offerId, sequenceIndex);
            } catch (Exception e) {
                log.error("Error during dispatch offer timeout check for Offer #{}: {}", offerId, e.getMessage(), e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    @Transactional
    public void handleOfferTimeout(Integer orderId, Long riderId, Long offerId, int sequenceIndex) {
        Optional<RiderDispatchOffer> offerOpt = riderDispatchOfferRepository.findById(offerId);
        if (offerOpt.isEmpty()) return;

        RiderDispatchOffer offer = offerOpt.get();
        if ("OFFERED".equals(offer.getStatus())) {
            log.info("Offer #{} for Rider #{} on Order #{} timed out without response. Cascading to next rider...",
                    offerId, riderId, orderId);

            offer.setStatus("EXPIRED");
            offer.setRespondedAt(LocalDateTime.now());
            riderDispatchOfferRepository.save(offer);

            // Cascade to the next nearby rider
            offerNextRider(orderId, sequenceIndex + 1);
        }
    }

    @Override
    @Transactional
    public boolean respondToOffer(Long riderId, Long offerId, RiderOfferRespondRequest request) {
        RiderDispatchOffer offer = riderDispatchOfferRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch offer not found with ID: " + offerId));

        if (!offer.getRider().getId().equals(riderId)) {
            throw new IllegalArgumentException("Rider ID mismatch for offer #" + offerId);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!"OFFERED".equals(offer.getStatus()) || (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(now))) {
            if ("OFFERED".equals(offer.getStatus())) {
                offer.setStatus("EXPIRED");
                offer.setRespondedAt(now);
                riderDispatchOfferRepository.save(offer);
            }
            log.warn("Offer #{} is no longer active (status: {})", offerId, offer.getStatus());
            return false;
        }

        Integer orderId = offer.getOrder().getOrderId();

        if ("REJECT".equalsIgnoreCase(request.getAction())) {
            offer.setStatus("REJECTED");
            offer.setRejectionReason(request.getReason() != null ? request.getReason() : "Rejected by rider");
            offer.setRespondedAt(now);
            riderDispatchOfferRepository.save(offer);

            log.info("Rider #{} REJECTED Offer #{} for Order #{}. Cascading immediately to next rider...",
                    riderId, offerId, orderId);

            // Cascade immediately to next rider in the sequence
            offerNextRider(orderId, offer.getSequenceIndex() + 1);
            return true;
        }

        if ("ACCEPT".equalsIgnoreCase(request.getAction())) {
            DeliveryRider rider = offer.getRider();
            String vehicleNumber = rider.getVehicleNumber() != null ? rider.getVehicleNumber() : "2-Wheeler";

            // Perform atomic database assignment
            int rowsUpdated = orderRepository.atomicAssignRiderToOrder(
                    orderId,
                    rider.getId(),
                    rider.getName(),
                    rider.getPhone(),
                    vehicleNumber,
                    now
            );

            if (rowsUpdated == 1) {
                // Winner!
                offer.setStatus("ACCEPTED");
                offer.setRespondedAt(now);
                riderDispatchOfferRepository.save(offer);

                rider.setActiveOrderId(orderId);
                rider.setAvailable(false);
                deliveryRiderRepository.save(rider);

                // Cancel all other pending offers for this order
                riderDispatchOfferRepository.cancelPendingOffersForOrder(orderId, now);

                // Add tracking checkpoint
                Order order = offer.getOrder();
                TrackingCheckpoint cp = TrackingCheckpoint.builder()
                        .order(order)
                        .status("RIDER_ASSIGNED")
                        .title("Rider Assigned")
                        .location(order.getStore() != null ? order.getStore().getName() : "Local Hub")
                        .description("Delivery partner " + rider.getName() + " (" + rider.getPhone() + ") assigned and en route to store.")
                        .timestamp(now)
                        .build();
                order.getCheckpoints().add(cp);
                orderRepository.save(order);

                log.info("SUCCESS: Rider #{} ('{}') accepted and assigned to Order #{}",
                        rider.getId(), rider.getName(), orderId);
                return true;
            } else {
                // Already assigned or locked
                offer.setStatus("EXPIRED");
                offer.setRespondedAt(now);
                riderDispatchOfferRepository.save(offer);
                log.info("Offer #{} ACCEPT rejected: Order #{} was already assigned or cancelled.", offerId, orderId);
                return false;
            }
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RiderOfferResponse> getActiveOfferForRider(Long riderId) {
        LocalDateTime now = LocalDateTime.now();
        return riderDispatchOfferRepository.findFirstByRider_IdAndStatusOrderByOfferedAtDesc(riderId, "OFFERED")
                .filter(o -> o.getExpiresAt().isAfter(now))
                .map(o -> {
                    Order order = o.getOrder();
                    Store store = order.getStore();
                    long remaining = Duration.between(now, o.getExpiresAt()).getSeconds();

                    Double storeLat = store != null ? store.getLatitude() : null;
                    Double storeLng = store != null ? store.getLongitude() : null;

                    Double delivLat = null;
                    Double delivLng = null;
                    if (addressRepository != null && order.getAddressId() != null) {
                        Address addr = addressRepository.findById(order.getAddressId()).orElse(null);
                        if (addr != null) {
                            delivLat = addr.getLatitude();
                            delivLng = addr.getLongitude();
                        }
                    }

                    String pickupNavUrl = (storeLat != null && storeLng != null)
                            ? String.format(Locale.ROOT, "https://www.google.com/maps/dir/?api=1&destination=%.6f,%.6f", storeLat, storeLng)
                            : null;

                    String delivNavUrl = (delivLat != null && delivLng != null)
                            ? String.format(Locale.ROOT, "https://www.google.com/maps/dir/?api=1&destination=%.6f,%.6f", delivLat, delivLng)
                            : null;

                    return RiderOfferResponse.builder()
                            .offerId(o.getId())
                            .orderId(order.getOrderId())
                            .orderNumber(order.getOrderNumber())
                            .sequenceIndex(o.getSequenceIndex())
                            .distanceKm(o.getDistanceKm())
                            .offeredFare(o.getOfferedFare())
                            .status(o.getStatus())
                            .offeredAt(o.getOfferedAt())
                            .expiresAt(o.getExpiresAt())
                            .remainingSeconds(Math.max(0, remaining))
                            .storeName(store != null ? store.getName() : "HinchStore")
                            .storeLatitude(storeLat)
                            .storeLongitude(storeLng)
                            .deliveryLocation(order.getDeliveryLocation())
                            .deliveryLatitude(delivLat)
                            .deliveryLongitude(delivLng)
                            .pickupNavigationUrl(pickupNavUrl)
                            .deliveryNavigationUrl(delivNavUrl)
                            .totalAmount(order.getTotalAmount())
                            .paymentMethod(order.getPaymentMethod())
                            .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                            .build();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderResponse> getActiveAssignedOrder(Long riderId) {
        if (riderId == null) {
            return Optional.empty();
        }
        Optional<DeliveryRider> riderOpt = deliveryRiderRepository.findById(riderId);
        if (riderOpt.isEmpty()) {
            return Optional.empty();
        }
        DeliveryRider rider = riderOpt.get();
        Integer activeOrderId = rider.getActiveOrderId();
        if (activeOrderId == null) {
            return Optional.empty();
        }

        Optional<Order> orderOpt = orderRepository.findById(activeOrderId);
        if (orderOpt.isEmpty()) {
            return Optional.empty();
        }
        Order order = orderOpt.get();

        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus())
                || "CANCELLED".equalsIgnoreCase(order.getOrderStatus())
                || "REJECTED_BY_SELLER".equalsIgnoreCase(order.getOrderStatus())) {
            return Optional.empty();
        }

        if (orderService != null) {
            return Optional.of(orderService.getOrderById(order.getOrderId()));
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public DeliveryRider updateRiderStatus(Long riderId, RiderLocationUpdateRequest request) {
        DeliveryRider rider = deliveryRiderRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found with ID: " + riderId));

        if (request.getLatitude() != null) {
            rider.setCurrentLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            rider.setCurrentLongitude(request.getLongitude());
        }
        if (request.getIsOnline() != null) {
            rider.setOnline(request.getIsOnline());
        }
        if (request.getFcmToken() != null) {
            rider.setFcmToken(request.getFcmToken());
        }
        rider.setLastLocationUpdate(LocalDateTime.now());
        return deliveryRiderRepository.save(rider);
    }

    @Override
    public double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round((6371 * c) * 100.0) / 100.0;
    }

    private BigDecimal calculateFare(String vehicleTypeCode, double distanceKm) {
        if (vehicleTypeService != null && vehicleTypeCode != null && !vehicleTypeCode.isBlank()) {
            try {
                com.example.project.customer.entity.VehicleType vt = vehicleTypeService.findEntityByCode(vehicleTypeCode);
                if (vt != null) {
                    return vehicleTypeService.calculateFare(vt, distanceKm);
                }
            } catch (Exception ignored) {}
        }
        // Base fallback fare ₹40 + ₹12 per km
        double fare = 40.0 + (distanceKm * 12.0);
        return BigDecimal.valueOf(fare).setScale(2, RoundingMode.HALF_UP);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}
