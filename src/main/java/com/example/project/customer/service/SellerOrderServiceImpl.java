package com.example.project.customer.service;

import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.SellerOrderRejectRequest;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.PayoutLedgerStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderServiceImpl implements SellerOrderService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final SellerPayoutLedgerRepository sellerPayoutLedgerRepository;
    private final OrderService orderService;
    private final VehicleTypeService vehicleTypeService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getSellerOrders(Integer sellerId, String status, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit));

        Page<Order> orders;
        if (status != null && !status.isBlank()) {
            orders = orderRepository.findByStore_Seller_SellerIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(
                    sellerId, status.trim(), pageable);
        } else {
            orders = orderRepository.findByStore_Seller_SellerIdOrderByCreatedAtDesc(sellerId, pageable);
        }

        return orders.map(o -> orderService.getOrderById(o.getOrderId()));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getSellerOrderById(Integer sellerId, Integer orderId) {
        Order order = findAndValidateSellerOrder(sellerId, orderId);
        return orderService.getOrderById(order.getOrderId());
    }

    @Override
    @Transactional
    public OrderResponse acceptOrder(Integer sellerId, Integer orderId, com.example.project.customer.dto.SellerOrderAcceptRequest request) {
        Order order = findAndValidateSellerOrder(sellerId, orderId);

        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus()) || "REJECTED_BY_SELLER".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Cannot accept order #" + orderId + " because it is already " + order.getOrderStatus());
        }

        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus()) || "ACCEPTED_BY_SELLER".equalsIgnoreCase(order.getOrderStatus())) {
            return orderService.getOrderById(order.getOrderId());
        }

        // 1. Resolve seller-selected vehicle type (or auto-assign based on order weight)
        String selectedVehicle = null;
        if (request != null && request.getVehicleTypeCode() != null && !request.getVehicleTypeCode().isBlank()) {
            String code = request.getVehicleTypeCode().trim().toUpperCase();
            if (vehicleTypeService != null) {
                com.example.project.customer.entity.VehicleType vt = vehicleTypeService.findEntityByCode(code);
                selectedVehicle = (vt != null) ? vt.getCode() : code;
            } else {
                selectedVehicle = code;
            }
        } else {
            selectedVehicle = autoSelectVehicleType(order.getTotalWeightKg());
        }
        order.setSelectedVehicleType(selectedVehicle);

        // 2. Generate 6-digit cryptographically secure Customer Delivery OTP
        int otpNumber = 100000 + secureRandom.nextInt(900000);
        String deliveryOtp = String.valueOf(otpNumber);

        LocalDateTime now = LocalDateTime.now();
        order.setDeliveryOtp(deliveryOtp);
        order.setDeliveryOtpGeneratedAt(now);
        order.setDeliveryOtpAttempts(0);
        order.setSellerAcceptedAt(now);
        order.setOrderStatus("ACCEPTED_BY_SELLER");

        String storeName = order.getStore() != null ? order.getStore().getName() : "Seller Store";
        TrackingCheckpoint cp = TrackingCheckpoint.builder()
                .order(order)
                .status("ACCEPTED_BY_SELLER")
                .title("Order Accepted by Seller (" + selectedVehicle + ")")
                .location(storeName)
                .description("Order accepted and being packed by " + storeName + ". Vehicle required: " + selectedVehicle + ". Delivery OTP generated for customer.")
                .timestamp(now)
                .build();
        order.getCheckpoints().add(cp);

        Order saved = orderRepository.save(order);
        log.info("Seller #{} ACCEPTED Order #{}. Vehicle selected: '{}'. Generated Delivery OTP.",
                sellerId, orderId, selectedVehicle);

        return orderService.getOrderById(saved.getOrderId());
    }

    @Override
    @Transactional
    public OrderResponse acceptOrder(Integer sellerId, Integer orderId) {
        return acceptOrder(sellerId, orderId, null);
    }

    private String autoSelectVehicleType(java.math.BigDecimal weightKg) {
        if (weightKg == null) return "TWO_WHEELER";
        double w = weightKg.doubleValue();
        if (w <= 20.0) return "TWO_WHEELER";
        if (w <= 500.0) return "THREE_WHEELER";
        if (w <= 1000.0) return "TATA_ACE";
        if (w <= 1750.0) return "PICKUP_8FT";
        return "TATA_407";
    }

    @Override
    @Transactional
    public OrderResponse rejectOrder(Integer sellerId, Integer orderId, SellerOrderRejectRequest request) {
        Order order = findAndValidateSellerOrder(sellerId, orderId);

        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus()) || "IN_TRANSIT".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Cannot reject order #" + orderId + " while in status " + order.getOrderStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        order.setOrderStatus("REJECTED_BY_SELLER");
        order.setSellerRejectionReason(request.getReason());
        order.setPaymentStatus("REFUND_PENDING");

        // Restore inventory stock
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                restoreStock(item);
            }
        }

        // Reverse Seller Payout Ledger
        sellerPayoutLedgerRepository.findByOrder_OrderId(orderId).ifPresent(ledger -> {
            ledger.setStatus(PayoutLedgerStatus.REVERSED);
            sellerPayoutLedgerRepository.save(ledger);
        });

        String storeName = order.getStore() != null ? order.getStore().getName() : "Seller Store";
        TrackingCheckpoint cp = TrackingCheckpoint.builder()
                .order(order)
                .status("REJECTED_BY_SELLER")
                .title("Order Rejected by Seller")
                .location(storeName)
                .description("Order rejected by " + storeName + ". Reason: " + request.getReason())
                .timestamp(now)
                .build();
        order.getCheckpoints().add(cp);

        Order saved = orderRepository.save(order);
        log.info("Seller #{} REJECTED Order #{}. Reason: {}", sellerId, orderId, request.getReason());

        return orderService.getOrderById(saved.getOrderId());
    }

    private Order findAndValidateSellerOrder(Integer sellerId, Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        Store store = order.getStore();
        if (store == null || store.getSeller() == null || !store.getSeller().getSellerId().equals(sellerId)) {
            // Also permit if sellerId is 1 (default store) or admin
            if (sellerId != null && !sellerId.equals(1) && !com.example.project.customer.security.SecurityUtils.isAdmin()) {
                throw new ForbiddenException("Access Denied: Order #" + orderId + " does not belong to your store.");
            }
        }

        return order;
    }

    private void restoreStock(OrderItem item) {
        if (item.getProductId() != null && item.getQuantity() != null) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int current = product.getStockQty() != null ? product.getStockQty() : 0;
                product.setStockQty(current + item.getQuantity());
                productRepository.save(product);
                log.info("Restored {} stock for product #{}", item.getQuantity(), product.getProductId());
            });
        }
    }
}
