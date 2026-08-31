package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.InvoiceResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.OrderSummaryResponse;
import com.example.project.customer.dto.OrderTrackingResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.entity.UserProfile;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserProfileRepository userProfileRepository;
    private final CartService cartService;
    private final CheckoutService checkoutService;

    @Override
    public OrderResponse createOrder(Integer userId, OrderCreateRequest request) {
        int uid = userId != null ? userId : 101;
        CartResponse cart = cartService.getCart(uid);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot place order with an empty cart");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));

        CheckoutPreviewRequest previewReq = CheckoutPreviewRequest.builder()
                .addressId(request.getAddressId())
                .deliverySlot(request.getDeliverySlot())
                .requiresCraneUnloading(request.getRequiresCraneUnloading())
                .build();

        CheckoutPreviewResponse preview = checkoutService.previewCheckout(uid, previewReq);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long randomSuffix = (long) (Math.random() * 900) + 100;
        String orderNumber = "ORD-" + dateStr + "-" + randomSuffix;

        String formattedAddress = address.getSiteName() + ", " + address.getAddressLine1() + ", "
                + address.getCity() + ", " + address.getState() + " - " + address.getPincode();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(uid)
                .addressId(address.getId())
                .deliveryLocation(formattedAddress)
                .subtotal(preview.getSubtotal())
                .discount(preview.getDiscount())
                .couponCode(cart.getAppliedCoupon())
                .taxableAmount(preview.getTaxableAmount())
                .cgst(preview.getCgst())
                .sgst(preview.getSgst())
                .igst(preview.getIgst())
                .totalGst(preview.getTotalGst())
                .freightCharge(preview.getFreightCharge())
                .craneUnloadingCharge(preview.getCraneUnloadingCharge())
                .totalAmount(preview.getGrandTotal())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "RAZORPAY")
                .paymentStatus("PENDING")
                .orderStatus("PLACED")
                .poNumber(request.getPoNumber())
                .deliverySlot(request.getDeliverySlot())
                .deliveryInstructions(request.getDeliveryInstructions())
                .requiresCraneUnloading(Boolean.TRUE.equals(request.getRequiresCraneUnloading()))
                .carrierName("VRL Logistics Heavy Freight Fleet")
                .vehicleNumber("TS 09 UB 4412 (22-Wheel Flatbed)")
                .driverName("Ramesh Yadav (+91 9849012345)")
                .trackingNumber("VRL-HYD-" + dateStr + "-" + randomSuffix)
                .estimatedDelivery(LocalDateTime.now().plusDays(1))
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemResponse ci : cart.getItems()) {
            decrementStock(ci);

            OrderItem oi = OrderItem.builder()
                    .order(savedOrder)
                    .productId(ci.getProductId())
                    .title(ci.getTitle())
                    .imageUrl(ci.getImageUrl())
                    .quantity(ci.getQuantity())
                    .unit(ci.getUnit())
                    .unitPrice(ci.getUnitPrice())
                    .originalPrice(ci.getOriginalPrice())
                    .appliedTier(ci.getAppliedTier())
                    .gstRate(ci.getGstRate())
                    .lineTotal(ci.getLineTotal())
                    .lineGst(ci.getLineGst())
                    .build();
            orderItems.add(orderItemRepository.save(oi));
        }
        savedOrder.setItems(orderItems);

        // Initial tracking checkpoint
        TrackingCheckpoint initialCheckpoint = TrackingCheckpoint.builder()
                .order(savedOrder)
                .status("ORDER_PLACED")
                .title("Order Placed & Verified")
                .location("HinchMart Hyderabad Central Ops")
                .description("Order confirmed and routed to Tata Steel Distribution Hub.")
                .timestamp(LocalDateTime.now())
                .build();
        savedOrder.getCheckpoints().add(initialCheckpoint);
        orderRepository.save(savedOrder);

        // Clear active cart
        cartService.clearCart(uid);

        return mapToOrderResponse(savedOrder);
    }

    /**
     * Uses a database lock so simultaneous checkouts cannot oversell a product.
     * Cancellation deliberately does not restore stock: stock changes only when
     * the order is placed.
     */
    private void decrementStock(CartItemResponse cartItem) {
        if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
            throw new IllegalStateException("Order item quantity must be greater than zero");
        }

        Product product = productRepository.findByIdForStockUpdate(cartItem.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + cartItem.getProductId()));

        int availableStock = product.getStockQty() != null ? product.getStockQty() : 0;
        if (availableStock < cartItem.getQuantity()) {
            throw new ResourceConflictException(
                    "Insufficient stock for product id " + cartItem.getProductId()
                            + ". Available: " + availableStock);
        }

        product.setStockQty(availableStock - cartItem.getQuantity());
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<OrderSummaryResponse>> getOrders(Integer userId, String status, int page, int limit) {
        int uid = userId != null ? userId : 101;
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Order> orderPage;
        if (status != null && !status.isBlank()) {
            orderPage = orderRepository.findByUserIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(uid, status.trim(), pageable);
        } else {
            orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(uid, pageable);
        }

        List<OrderSummaryResponse> summaries = orderPage.getContent().stream()
                .map(this::mapToSummaryResponse).toList();

        PaginationMeta pagination = PaginationMeta.of(page > 0 ? page : 1, pageSize, orderPage.getTotalElements());
        return ApiResponse.paginated(summaries, pagination);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Integer id) {
        Order order = findOrder(id);
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse getOrderTracking(Integer id) {
        Order order = findOrder(id);

        List<OrderTrackingResponse.TrackingCheckpointDto> dtoList = order.getCheckpoints().stream()
                .map(cp -> OrderTrackingResponse.TrackingCheckpointDto.builder()
                        .status(cp.getStatus())
                        .title(cp.getTitle())
                        .location(cp.getLocation())
                        .timestamp(cp.getTimestamp())
                        .description(cp.getDescription())
                        .build())
                .toList();

        return OrderTrackingResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .carrierName(order.getCarrierName() != null ? order.getCarrierName() : "VRL Logistics Heavy Freight Fleet")
                .vehicleNumber(order.getVehicleNumber() != null ? order.getVehicleNumber() : "TS 09 UB 4412 (22-Wheel Flatbed)")
                .driverName(order.getDriverName() != null ? order.getDriverName() : "Ramesh Yadav (+91 9849012345)")
                .trackingNumber(order.getTrackingNumber() != null ? order.getTrackingNumber() : "VRL-HYD-" + order.getOrderId())
                .currentStatus(order.getOrderStatus())
                .estimatedDelivery(order.getEstimatedDelivery() != null ? order.getEstimatedDelivery() : order.getCreatedAt().plusDays(1))
                .checkpoints(dtoList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getOrderInvoice(Integer id) {
        Order order = findOrder(id);
        UserProfile user = userProfileRepository.findById(order.getUserId()).orElse(null);

        String buyerGstin = user != null && user.getGstNumber() != null ? user.getGstNumber() : "36AAACT2727Q1ZW";
        String buyerLegalName = user != null && user.getCompanyName() != null ? user.getCompanyName() : "Apex Infra Projects Pvt Ltd";

        String invoiceNum = "INV-" + LocalDate.now().getYear() + "-" + String.format("%06d", order.getOrderId());

        return InvoiceResponse.builder()
                .invoiceNumber(invoiceNum)
                .orderNumber(order.getOrderNumber())
                .invoiceDate(order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : LocalDate.now())
                .sellerGstin("36AAACH2026Q1Z1")
                .sellerLegalName("HinchMart B2B Commerce Pvt Ltd")
                .buyerGstin(buyerGstin)
                .buyerLegalName(buyerLegalName)
                .taxableAmount(order.getTaxableAmount())
                .cgst(order.getCgst())
                .sgst(order.getSgst())
                .igst(order.getIgst())
                .grandTotal(order.getTotalAmount())
                .pdfUrl("https://cdn.hinchmart.com/invoices/" + invoiceNum + ".pdf")
                .build();
    }

    @Override
    public OrderResponse updateOrderStatus(Integer id, String status, String location, String description) {
        if ("CANCELLED".equalsIgnoreCase(status)) {
            return cancelOrder(id, location, description);
        }

        Order order = findOrder(id);
        order.setOrderStatus(status);

        TrackingCheckpoint checkpoint = TrackingCheckpoint.builder()
                .order(order)
                .status(status)
                .title(formatCheckpointTitle(status))
                .location(location != null ? location : "Outer Ring Road (ORR) Exit 11")
                .description(description != null ? description : "Trailer status updated to " + status)
                .timestamp(LocalDateTime.now())
                .build();

        order.getCheckpoints().add(checkpoint);
        return mapToOrderResponse(orderRepository.save(order));
    }

    @Override
    public OrderResponse cancelOrder(Integer id, String location, String description) {
        Order order = findOrder(id);

        // A repeated request must be idempotent: do not restore the same stock twice.
        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            return mapToOrderResponse(order);
        }

        for (OrderItem item : order.getItems()) {
            restoreStock(item);
        }

        order.setOrderStatus("CANCELLED");
        TrackingCheckpoint checkpoint = TrackingCheckpoint.builder()
                .order(order)
                .status("CANCELLED")
                .title("Order Cancelled")
                .location(location != null ? location : "HinchMart Hyderabad Central Ops")
                .description(description != null ? description : "Order cancelled and product stock restored")
                .timestamp(LocalDateTime.now())
                .build();
        order.getCheckpoints().add(checkpoint);

        return mapToOrderResponse(orderRepository.save(order));
    }

    private void restoreStock(OrderItem orderItem) {
        if (orderItem.getQuantity() == null || orderItem.getQuantity() <= 0) {
            throw new IllegalStateException("Order item quantity must be greater than zero");
        }

        Product product = productRepository.findByIdForStockUpdate(orderItem.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + orderItem.getProductId()));

        int availableStock = product.getStockQty() != null ? product.getStockQty() : 0;
        product.setStockQty(availableStock + orderItem.getQuantity());
        productRepository.save(product);
    }

    private Order findOrder(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private String formatCheckpointTitle(String status) {
        return switch (status.toUpperCase()) {
            case "LOADED" -> "Loaded & Weighed at Weighbridge";
            case "DISPATCHED" -> "Dispatched from Distribution Hub";
            case "IN_TRANSIT" -> "Vehicle En Route to Jobsite";
            case "DELIVERED" -> "Delivered and Unloaded at Jobsite";
            case "CANCELLED" -> "Order Cancelled";
            default -> "Status Updated: " + status;
        };
    }

    private OrderSummaryResponse mapToSummaryResponse(Order o) {
        String firstTitle = null;
        String firstImage = null;
        int count = o.getItems() != null ? o.getItems().size() : 0;

        if (o.getItems() != null && !o.getItems().isEmpty()) {
            OrderItem first = o.getItems().get(0);
            firstTitle = first.getTitle();
            firstImage = first.getImageUrl();
        }

        return OrderSummaryResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .totalAmount(o.getTotalAmount())
                .orderStatus(o.getOrderStatus())
                .paymentStatus(o.getPaymentStatus())
                .itemCount(count)
                .firstItemTitle(firstTitle)
                .firstItemImage(firstImage)
                .createdAt(o.getCreatedAt())
                .estimatedDelivery(o.getEstimatedDelivery())
                .build();
    }

    private OrderResponse mapToOrderResponse(Order o) {
        List<OrderResponse.OrderItemDto> itemDtos = o.getItems() != null ? o.getItems().stream()
                .map(i -> OrderResponse.OrderItemDto.builder()
                        .orderItemId(i.getOrderItemId())
                        .productId(i.getProductId())
                        .title(i.getTitle())
                        .imageUrl(i.getImageUrl())
                        .quantity(i.getQuantity())
                        .unit(i.getUnit())
                        .unitPrice(i.getUnitPrice())
                        .originalPrice(i.getOriginalPrice())
                        .appliedTier(i.getAppliedTier())
                        .gstRate(i.getGstRate())
                        .lineTotal(i.getLineTotal())
                        .lineGst(i.getLineGst())
                        .build())
                .toList() : new ArrayList<>();

        String firstTitle = itemDtos.isEmpty() ? null : itemDtos.get(0).getTitle();
        String firstImage = itemDtos.isEmpty() ? null : itemDtos.get(0).getImageUrl();

        return OrderResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .totalAmount(o.getTotalAmount())
                .subtotal(o.getSubtotal())
                .discount(o.getDiscount())
                .taxableAmount(o.getTaxableAmount())
                .cgst(o.getCgst())
                .sgst(o.getSgst())
                .igst(o.getIgst())
                .totalGst(o.getTotalGst())
                .freightCharge(o.getFreightCharge())
                .craneUnloadingCharge(o.getCraneUnloadingCharge())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .orderStatus(o.getOrderStatus())
                .poNumber(o.getPoNumber())
                .deliverySlot(o.getDeliverySlot())
                .deliveryInstructions(o.getDeliveryInstructions())
                .requiresCraneUnloading(o.isRequiresCraneUnloading())
                .itemCount(itemDtos.size())
                .firstItemTitle(firstTitle)
                .firstItemImage(firstImage)
                .items(itemDtos)
                .createdAt(o.getCreatedAt())
                .estimatedDelivery(o.getEstimatedDelivery())
                .build();
    }
}
