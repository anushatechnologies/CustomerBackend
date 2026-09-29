package com.example.project.customer.service;

import com.example.project.customer.dto.DeliveryVerificationRequest;
import com.example.project.customer.dto.DeliveryVerificationResponse;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.entity.DeliveryRider;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.PayoutLedgerStatus;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.repository.DeliveryRiderRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryVerificationServiceImpl implements DeliveryVerificationService {

    private final OrderRepository orderRepository;
    private final DeliveryRiderRepository deliveryRiderRepository;
    private final SellerPayoutLedgerRepository sellerPayoutLedgerRepository;
    private final OrderService orderService;

    private static final int MAX_OTP_ATTEMPTS = 3;

    @Override
    @Transactional
    public OrderResponse confirmPickup(Long riderId, Integer orderId) {
        Order order = findAndValidateAssignedOrder(riderId, orderId);

        order.setOrderStatus("IN_TRANSIT");
        order.setPickedUpAt(LocalDateTime.now());

        String storeName = order.getStore() != null ? order.getStore().getName() : "Seller Store";
        TrackingCheckpoint cp = TrackingCheckpoint.builder()
                .order(order)
                .status("IN_TRANSIT")
                .title("Order Picked Up")
                .location(storeName)
                .description("Package collected by rider " + order.getDriverName() + " and on the way to customer delivery address.")
                .timestamp(LocalDateTime.now())
                .build();
        order.getCheckpoints().add(cp);

        Order saved = orderRepository.save(order);
        log.info("Rider #{} confirmed pickup for Order #{}", riderId, orderId);
        return orderService.getOrderById(saved.getOrderId());
    }

    @Override
    @Transactional
    public OrderResponse markArrivedAtCustomer(Long riderId, Integer orderId) {
        Order order = findAndValidateAssignedOrder(riderId, orderId);

        order.setOrderStatus("OUT_FOR_DELIVERY");

        TrackingCheckpoint cp = TrackingCheckpoint.builder()
                .order(order)
                .status("OUT_FOR_DELIVERY")
                .title("Rider Arrived at Delivery Site")
                .location(order.getDeliveryLocation() != null ? order.getDeliveryLocation() : "Customer Site")
                .description("Rider is at customer destination. Awaiting OTP and payment verification.")
                .timestamp(LocalDateTime.now())
                .build();
        order.getCheckpoints().add(cp);

        Order saved = orderRepository.save(order);
        log.info("Rider #{} arrived at customer destination for Order #{}", riderId, orderId);
        return orderService.getOrderById(saved.getOrderId());
    }

    @Override
    @Transactional
    public DeliveryVerificationResponse verifyAndCompleteDelivery(Long riderId, Integer orderId, DeliveryVerificationRequest request) {
        Order order = findAndValidateAssignedOrder(riderId, orderId);

        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            return DeliveryVerificationResponse.builder()
                    .success(true)
                    .message("Order is already delivered.")
                    .orderId(order.getOrderId())
                    .orderNumber(order.getOrderNumber())
                    .orderStatus(order.getOrderStatus())
                    .paymentStatus(order.getPaymentStatus())
                    .deliveredAt(order.getDeliveredAt())
                    .build();
        }

        // 1. OTP Verification & Brute-force rate-limiting
        if (order.getDeliveryOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new IllegalStateException("Delivery OTP verification is locked due to too many failed attempts ("
                    + MAX_OTP_ATTEMPTS + "). Please contact customer support.");
        }

        String storedOtp = order.getDeliveryOtp();
        String submittedOtp = request.getOtp() != null ? request.getOtp().trim() : "";

        if (storedOtp == null || !storedOtp.equals(submittedOtp)) {
            int newAttempts = order.getDeliveryOtpAttempts() + 1;
            order.setDeliveryOtpAttempts(newAttempts);
            orderRepository.save(order);

            int remaining = MAX_OTP_ATTEMPTS - newAttempts;
            log.warn("Invalid OTP '{}' entered for Order #{} by Rider #{}. Attempts used: {}/{}",
                    submittedOtp, orderId, riderId, newAttempts, MAX_OTP_ATTEMPTS);

            if (remaining > 0) {
                throw new IllegalArgumentException("Invalid Delivery OTP. " + remaining + " attempt(s) remaining.");
            } else {
                throw new IllegalStateException("Invalid Delivery OTP. Maximum attempts reached. Verification locked.");
            }
        }

        // 2. Payment Verification
        boolean isPrepaid = "PAID".equalsIgnoreCase(order.getPaymentStatus());
        if (!isPrepaid) {
            // Cash on Delivery / Pay on Delivery validation
            BigDecimal dueAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal collectedAmount = request.getCodAmountCollected() != null ? request.getCodAmountCollected() : BigDecimal.ZERO;

            if (collectedAmount.compareTo(dueAmount) < 0) {
                throw new IllegalArgumentException("Incomplete payment collected. Total order amount is ₹"
                        + dueAmount + ", but only ₹" + collectedAmount + " was reported as collected.");
            }

            order.setPaymentStatus("PAID");
            order.setCodAmountCollected(collectedAmount);
            log.info("Payment confirmed via COD for Order #{}: ₹{}", orderId, collectedAmount);
        }

        // 3. Complete Delivery
        LocalDateTime now = LocalDateTime.now();
        order.setOrderStatus("DELIVERED");
        order.setDeliveryOtpVerifiedAt(now);
        order.setDeliveredAt(now);

        TrackingCheckpoint cp = TrackingCheckpoint.builder()
                .order(order)
                .status("DELIVERED")
                .title("Order Delivered & Verified")
                .location(order.getDeliveryLocation() != null ? order.getDeliveryLocation() : "Customer Site")
                .description("Delivery confirmed successfully with Customer OTP and Payment verification.")
                .timestamp(now)
                .build();
        order.getCheckpoints().add(cp);
        orderRepository.save(order);

        // 4. Release Rider
        deliveryRiderRepository.findById(riderId).ifPresent(rider -> {
            rider.setActiveOrderId(null);
            rider.setAvailable(true);
            deliveryRiderRepository.save(rider);
        });

        // 5. Release Seller Payout in Ledger
        sellerPayoutLedgerRepository.findByOrder_OrderId(orderId).ifPresent(ledger -> {
            ledger.setStatus(PayoutLedgerStatus.PENDING);
            ledger.setSettlementDate(now.plusDays(7)); // Eligible for T+7 settlement
            sellerPayoutLedgerRepository.save(ledger);
            log.info("Seller payout ledger #{} moved to PENDING for settlement", ledger.getId());
        });

        log.info("SUCCESS: Order #{} successfully DELIVERED and verified by Rider #{}", orderId, riderId);

        return DeliveryVerificationResponse.builder()
                .success(true)
                .message("Order delivered and verified successfully.")
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .orderStatus("DELIVERED")
                .paymentStatus(order.getPaymentStatus())
                .deliveredAt(now)
                .build();
    }

    private Order findAndValidateAssignedOrder(Long riderId, Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getRiderId() == null || !order.getRiderId().equals(riderId)) {
            throw new UnauthorizedException("Order #" + orderId + " is not assigned to Rider #" + riderId);
        }

        return order;
    }
}
