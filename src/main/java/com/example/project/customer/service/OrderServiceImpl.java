package com.example.project.customer.service;

import com.example.project.customer.common.PagedResponse;
import com.example.project.customer.dto.*;
import com.example.project.customer.entity.*;
import com.example.project.customer.exception.BadRequestException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final UserProfileRepository userProfileRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            AddressRepository addressRepository,
                            CartService cartService,
                            CartRepository cartRepository,
                            UserProfileRepository userProfileRepository) {
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.cartService = cartService;
        this.cartRepository = cartRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public OrderResponse placeOrder(Integer userId, CreateOrderRequest request) {
        Integer uid = userId != null ? userId : 101;
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));

        CartDto cartDto = cartService.getCart(uid);
        if (cartDto.getItems() == null || cartDto.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place order with an empty cart");
        }

        BigDecimal subtotal = cartDto.getSubtotal();
        BigDecimal discount = cartDto.getCouponDiscount() != null ? cartDto.getCouponDiscount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discount);
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        boolean isIntrastate = address.getState() != null &&
                address.getState().trim().equalsIgnoreCase("Telangana");

        BigDecimal totalGst = taxableAmount.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cgst = isIntrastate ? taxableAmount.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal sgst = isIntrastate ? taxableAmount.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal igst = isIntrastate ? BigDecimal.ZERO : totalGst;

        BigDecimal freightCharge = new BigDecimal("4500.00");
        BigDecimal craneCharge = request.isRequiresCraneUnloading() ? new BigDecimal("2500.00") : BigDecimal.ZERO;
        BigDecimal grandTotal = taxableAmount.add(totalGst).add(freightCharge).add(craneCharge);

        Order order = new Order();
        order.setUserId(uid);
        order.setAddress(address);
        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setTaxableAmount(taxableAmount);
        order.setCgst(cgst);
        order.setSgst(sgst);
        order.setIgst(igst);
        order.setTotalGst(totalGst);
        order.setFreightCharge(freightCharge);
        order.setCraneUnloadingCharge(craneCharge);
        order.setTotalAmount(grandTotal);
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "RAZORPAY");
        order.setPaymentStatus("PENDING");
        order.setOrderStatus("PLACED");
        order.setDeliverySlot(request.getDeliverySlot() != null ? request.getDeliverySlot() : "TOMORROW_MORNING");
        order.setDeliveryInstructions(request.getDeliveryInstructions());
        order.setPoNumber(request.getPoNumber());
        order.setRequiresCraneUnloading(request.isRequiresCraneUnloading());
        order.setEstimatedDelivery(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        // Logistics defaults
        order.setCarrierName("VRL Logistics Heavy Freight Fleet");
        order.setVehicleNumber("TS 09 UB 4412 (22-Wheel Flatbed)");
        order.setDriverName("Ramesh Yadav (+91 9849012345)");

        // Buyer details
        UserProfile profile = userProfileRepository.findById(uid).orElse(null);
        if (profile != null) {
            order.setBuyerLegalName(profile.getCompanyName() != null ? profile.getCompanyName() : profile.getFullName());
            order.setBuyerGstin(profile.getGstNumber() != null ? profile.getGstNumber() : "36AAACT2727Q1ZW");
        }

        // Temporary order number placeholder before saving
        String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        long tempId = System.currentTimeMillis() % 10000;
        order.setOrderNumber("ORD-" + dateStr + "-" + tempId);

        Order saved = orderRepository.save(order);

        // Update finalized orderNumber and invoiceNumber with entity ID
        String finalOrderNumber = "ORD-" + dateStr + "-" + saved.getOrderId();
        String invoiceNumber = "INV-" + LocalDate.now().getYear() + "-" + String.format("%06d", saved.getOrderId());
        String pdfUrl = "https://cdn.hinchmart.com/invoices/" + invoiceNumber + ".pdf";

        saved.setOrderNumber(finalOrderNumber);
        saved.setInvoiceNumber(invoiceNumber);
        saved.setPdfUrl(pdfUrl);
        saved.setTrackingNumber("VRL-HYD-" + LocalDate.now().getYear() + "-" + saved.getOrderId());

        // Add line items
        for (CartItemDto ci : cartDto.getItems()) {
            OrderItem oi = new OrderItem(
                    null,
                    ci.getTitle(),
                    ci.getImageUrl(),
                    ci.getQuantity(),
                    ci.getUnit(),
                    ci.getUnitPrice(),
                    ci.getLineTotal(),
                    ci.getGstRate(),
                    ci.getLineGst()
            );
            saved.addItem(oi);
        }

        // Add initial tracking checkpoint
        OrderTrackingCheckpoint cp = new OrderTrackingCheckpoint(
                "ORDER_PLACED",
                "Order Placed & Verified",
                "HinchMart Hyderabad Ops",
                LocalDateTime.now(),
                "Order confirmed and routed to Tata Steel Distribution Hub.",
                1
        );
        saved.addCheckpoint(cp);

        orderRepository.save(saved);

        // Clear cart
        cartService.clearCart(uid);

        return new OrderResponse(
                saved.getOrderId(),
                saved.getOrderNumber(),
                saved.getTotalAmount(),
                saved.getPaymentMethod(),
                saved.getPaymentStatus(),
                saved.getOrderStatus(),
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryDto> getOrders(Integer userId, String status, int page, int limit) {
        Integer uid = userId != null ? userId : 101;
        int pageIndex = Math.max(0, page - 1);
        int pageSize = limit > 0 ? limit : 20;

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            orders = orderRepository.findByUserIdAndOrderStatusIgnoreCase(uid, status.trim(), pageable);
        } else {
            orders = orderRepository.findByUserId(uid, pageable);
        }

        List<OrderSummaryDto> dtos = orders.getContent().stream().map(o -> {
            int itemCount = o.getItems() != null ? o.getItems().size() : 0;
            String firstTitle = (o.getItems() != null && !o.getItems().isEmpty()) ? o.getItems().get(0).getTitle() : "Procured Materials";
            String firstImage = (o.getItems() != null && !o.getItems().isEmpty()) ? o.getItems().get(0).getImageUrl() : null;

            return new OrderSummaryDto(
                    o.getOrderId(),
                    o.getOrderNumber(),
                    o.getTotalAmount(),
                    o.getOrderStatus(),
                    o.getPaymentStatus(),
                    itemCount,
                    firstTitle,
                    firstImage,
                    o.getCreatedAt(),
                    o.getEstimatedDelivery()
            );
        }).toList();

        return PagedResponse.of(dtos, page, pageSize, orders.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(Integer userId, Integer orderId) {
        Integer uid = userId != null ? userId : 101;
        Order o = orderRepository.findByOrderIdAndUserId(orderId, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderDetailResponse resp = new OrderDetailResponse();
        resp.setOrderId(o.getOrderId());
        resp.setOrderNumber(o.getOrderNumber());
        resp.setSubtotal(o.getSubtotal());
        resp.setDiscount(o.getDiscount());
        resp.setTaxableAmount(o.getTaxableAmount());
        resp.setCgst(o.getCgst());
        resp.setSgst(o.getSgst());
        resp.setIgst(o.getIgst());
        resp.setTotalGst(o.getTotalGst());
        resp.setFreightCharge(o.getFreightCharge());
        resp.setCraneUnloadingCharge(o.getCraneUnloadingCharge());
        resp.setTotalAmount(o.getTotalAmount());
        resp.setPaymentMethod(o.getPaymentMethod());
        resp.setPaymentStatus(o.getPaymentStatus());
        resp.setOrderStatus(o.getOrderStatus());
        resp.setDeliverySlot(o.getDeliverySlot());
        resp.setDeliveryInstructions(o.getDeliveryInstructions());
        resp.setPoNumber(o.getPoNumber());
        resp.setRequiresCraneUnloading(o.isRequiresCraneUnloading());
        resp.setEstimatedDelivery(o.getEstimatedDelivery());
        resp.setCarrierName(o.getCarrierName());
        resp.setVehicleNumber(o.getVehicleNumber());
        resp.setDriverName(o.getDriverName());
        resp.setTrackingNumber(o.getTrackingNumber());
        resp.setInvoiceNumber(o.getInvoiceNumber());
        resp.setPdfUrl(o.getPdfUrl());
        resp.setCreatedAt(o.getCreatedAt());

        if (o.getAddress() != null) {
            Address a = o.getAddress();
            resp.setAddress(new AddressResponse(
                    a.getAddressId(), a.getUserId(), a.getSiteName(), a.getRecipientName(),
                    a.getPhone(), a.getAddressLine1(), a.getCity(), a.getState(),
                    a.getPincode(), a.getLandmark(), a.isDefault(), a.isHasHeavyVehicleAccess(),
                    a.getCreatedAt()
            ));
        }

        if (o.getItems() != null) {
            List<CartItemDto> itemDtos = o.getItems().stream().map(i -> new CartItemDto(
                    i.getOrderItemId(),
                    i.getProduct() != null ? i.getProduct().getProductId() : null,
                    i.getTitle(),
                    i.getImageUrl(),
                    i.getQuantity(),
                    i.getUnit(),
                    i.getUnitPrice(),
                    i.getUnitPrice(),
                    null,
                    i.getGstRate(),
                    i.getLineTotal(),
                    i.getLineGst()
            )).toList();
            resp.setItems(itemDtos);
        }

        if (o.getCheckpoints() != null) {
            List<CheckpointDto> cpDtos = o.getCheckpoints().stream().map(cp -> new CheckpointDto(
                    cp.getStatus(),
                    cp.getTitle(),
                    cp.getLocation(),
                    cp.getTimestamp(),
                    cp.getDescription()
            )).toList();
            resp.setCheckpoints(cpDtos);
        }

        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse getOrderTracking(Integer userId, Integer orderId) {
        Integer uid = userId != null ? userId : 101;
        Order o = orderRepository.findByOrderIdAndUserId(orderId, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        List<CheckpointDto> cpDtos = new ArrayList<>();
        if (o.getCheckpoints() != null) {
            for (OrderTrackingCheckpoint cp : o.getCheckpoints()) {
                cpDtos.add(new CheckpointDto(
                        cp.getStatus(),
                        cp.getTitle(),
                        cp.getLocation(),
                        cp.getTimestamp(),
                        cp.getDescription()
                ));
            }
        }

        return new OrderTrackingResponse(
                o.getOrderId(),
                o.getOrderNumber(),
                o.getCarrierName() != null ? o.getCarrierName() : "VRL Logistics Heavy Freight Fleet",
                o.getVehicleNumber() != null ? o.getVehicleNumber() : "TS 09 UB 4412 (22-Wheel Flatbed)",
                o.getDriverName() != null ? o.getDriverName() : "Ramesh Yadav (+91 9849012345)",
                o.getTrackingNumber() != null ? o.getTrackingNumber() : "VRL-HYD-2026-" + o.getOrderId(),
                o.getOrderStatus(),
                o.getEstimatedDelivery(),
                cpDtos
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getOrderInvoice(Integer userId, Integer orderId) {
        Integer uid = userId != null ? userId : 101;
        Order o = orderRepository.findByOrderIdAndUserId(orderId, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        String invoiceNumber = o.getInvoiceNumber() != null ? o.getInvoiceNumber() : "INV-2026-000" + o.getOrderId();
        LocalDate invoiceDate = o.getInvoiceDate() != null ? o.getInvoiceDate() : (o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate() : LocalDate.now());
        String pdfUrl = o.getPdfUrl() != null ? o.getPdfUrl() : "https://cdn.hinchmart.com/invoices/" + invoiceNumber + ".pdf";

        return new InvoiceResponse(
                invoiceNumber,
                o.getOrderNumber(),
                invoiceDate,
                o.getSellerGstin(),
                o.getSellerLegalName(),
                o.getBuyerGstin(),
                o.getBuyerLegalName(),
                o.getTaxableAmount(),
                o.getCgst(),
                o.getSgst(),
                o.getIgst(),
                o.getTotalAmount(),
                pdfUrl
        );
    }
}
