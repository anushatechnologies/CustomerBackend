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
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.PayoutLedgerStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.SellerPayoutLedger;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private final CustomerRepository customerRepository;
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final PdfInvoiceGeneratorService pdfInvoiceGeneratorService;
    private final StoreInvoiceSequenceService storeInvoiceSequenceService;
    private final SellerPayoutLedgerRepository sellerPayoutLedgerRepository;
    private final StoreRepository storeRepository;
    private final UserContextUtil userContextUtil;
    private final SellerContextUtil sellerContextUtil;

    @Override
    public OrderResponse createOrder(Integer userId, OrderCreateRequest request) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        int uid = userId;
        CartResponse cart = cartService.getCart(uid);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot place order with an empty cart");
        }

        // 1. Resolve and validate store
        Integer storeId = cart.getStoreId() != null ? cart.getStoreId() : 1;
        Store store = storeRepository.findById(storeId)
                .orElseGet(() -> storeRepository.findById(1)
                        .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId)));

        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new IllegalStateException("Cannot checkout: Store '" + store.getName() + "' is currently " + store.getStatus() + " and not accepting orders.");
        }

        // 2. Pre-checkout stock & price re-validation
        for (CartItemResponse ci : cart.getItems()) {
            Product p = productRepository.findById(ci.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + ci.getTitle() + " (ID: " + ci.getProductId() + ")"));

            if (!p.isActive()) {
                throw new IllegalStateException("Product '" + p.getTitle() + "' is no longer active in the catalog.");
            }
            int availableStock = p.getStockQty() != null ? p.getStockQty() : 0;
            if (availableStock < ci.getQuantity()) {
                throw new ResourceConflictException("Insufficient stock for '" + p.getTitle() + "'. Requested: " + ci.getQuantity() + ", Available: " + availableStock);
            }
        }

        Address address = addressRepository.findByCustomer_CustomerIdAndId(uid, request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));

        CheckoutPreviewRequest previewReq = CheckoutPreviewRequest.builder()
                .addressId(request.getAddressId())
                .deliverySlot(request.getDeliverySlot())
                .requiresCraneUnloading(request.getRequiresCraneUnloading())
                .build();

        CheckoutPreviewResponse preview = checkoutService.previewCheckout(uid, previewReq);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String orderNumber = "ORD-" + dateStr + "-" + randomSuffix;

        StringBuilder sb = new StringBuilder();
        if (address.getSiteName() != null && !address.getSiteName().isBlank()) {
            sb.append(address.getSiteName()).append(", ");
        }
        if (address.getHouseFlatNo() != null && !address.getHouseFlatNo().isBlank()) {
            sb.append(address.getHouseFlatNo()).append(", ");
        }
        sb.append(address.getAddressLine1());
        if (address.getAddressLine2() != null && !address.getAddressLine2().isBlank()) {
            sb.append(", ").append(address.getAddressLine2());
        }
        if (address.getAreaLocality() != null && !address.getAreaLocality().isBlank()) {
            sb.append(", ").append(address.getAreaLocality());
        }
        sb.append(", ").append(address.getCity()).append(", ").append(address.getState()).append(" - ").append(address.getPincode());
        if (address.getRecipientName() != null && !address.getRecipientName().isBlank()) {
            sb.append(" (Contact: ").append(address.getRecipientName());
            if (address.getPhone() != null && !address.getPhone().isBlank()) {
                sb.append(" / ").append(address.getPhone());
            }
            sb.append(")");
        }
        String formattedAddress = sb.toString();

        // 3. Compute Commission snapshot
        BigDecimal commissionRate = store.getCommissionRate() != null ? store.getCommissionRate() : BigDecimal.valueOf(5.00);
        BigDecimal commissionAmount = preview.getTaxableAmount().multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // 4. Generate sequential GST Invoice Number per-store
        String storeInvoiceNumber = storeInvoiceSequenceService.generateNextInvoiceNumber(store);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(Customer.builder().customerId(uid).build())
                .store(store)
                .storeInvoiceNumber(storeInvoiceNumber)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
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

        // 5. Create SellerPayoutLedger entry (Gross - Commission - 1% TCS = Net Payout)
        BigDecimal tcsAmount = preview.getTaxableAmount().multiply(BigDecimal.valueOf(0.01)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossAmount = savedOrder.getTotalAmount();
        BigDecimal netPayout = grossAmount.subtract(commissionAmount).subtract(tcsAmount);

        SellerPayoutLedger ledger = SellerPayoutLedger.builder()
                .store(store)
                .order(savedOrder)
                .grossAmount(grossAmount)
                .commissionAmount(commissionAmount)
                .tcsAmount(tcsAmount)
                .netPayoutAmount(netPayout)
                .status(PayoutLedgerStatus.PENDING)
                .build();
        sellerPayoutLedgerRepository.save(ledger);

        // Initial tracking checkpoint
        TrackingCheckpoint initialCheckpoint = TrackingCheckpoint.builder()
                .order(savedOrder)
                .status("ORDER_PLACED")
                .title("Order Placed & Verified")
                .location(store.getName() + " Logistics Hub")
                .description("Order confirmed and assigned to " + store.getName() + " fulfillment team.")
                .timestamp(LocalDateTime.now())
                .build();
        savedOrder.getCheckpoints().add(initialCheckpoint);
        orderRepository.save(savedOrder);

        // Clear active cart
        cartService.clearCart(uid);

        log.info("Successfully created Order #{} (Invoice: {}) for Customer #{} from Store #{} ('{}')",
                savedOrder.getOrderId(), storeInvoiceNumber, uid, store.getStoreId(), store.getName());

        return mapToOrderResponse(savedOrder);
    }

    private synchronized void decrementStock(CartItemResponse cartItem) {
        Product product = productRepository.findByIdForStockUpdate(cartItem.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + cartItem.getProductId()));

        int currentStock = product.getStockQty() != null ? product.getStockQty() : 0;
        int requestedQty = cartItem.getQuantity();

        if (currentStock < requestedQty) {
            throw new ResourceConflictException("Insufficient stock for product '" + product.getTitle()
                    + "'. Available: " + currentStock + ", Requested: " + requestedQty);
        }

        product.setStockQty(currentStock - requestedQty);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<OrderSummaryResponse>> getOrders(Integer userId, String status, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        int uid = userId;
        Page<Order> orderPage = (status != null && !status.trim().isEmpty())
                ? orderRepository.findByCustomer_CustomerIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(uid, status.trim().toUpperCase(), pageable)
                : orderRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(uid, pageable);

        List<OrderSummaryResponse> summaryList = orderPage.getContent().stream()
                .map(this::mapToSummaryResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, orderPage.getTotalElements());
        return ApiResponse.paginated("Orders retrieved successfully", summaryList, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Integer id) {
        Order order = findOrder(id);
        validateOrderReadAccess(order);
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse getOrderTracking(Integer id) {
        Order order = findOrder(id);
        validateOrderReadAccess(order);
        List<OrderTrackingResponse.TrackingEvent> events = order.getCheckpoints().stream()
                .map(c -> OrderTrackingResponse.TrackingEvent.builder()
                        .checkpointId(c.getCheckpointId())
                        .status(c.getStatus())
                        .title(c.getTitle())
                        .location(c.getLocation())
                        .description(c.getDescription())
                        .timestamp(c.getTimestamp())
                        .build())
                .toList();

        return OrderTrackingResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .trackingNumber(order.getTrackingNumber())
                .carrierName(order.getCarrierName())
                .vehicleNumber(order.getVehicleNumber())
                .driverName(order.getDriverName())
                .estimatedDelivery(order.getEstimatedDelivery())
                .checkpoints(events)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getOrderInvoice(Integer id) {
        Order order = findOrder(id);
        validateOrderReadAccess(order);
        Customer customer = order.getCustomer();

        Store store = order.getStore();
        String sellerCompanyName = (store != null && store.getSeller() != null && store.getSeller().getCompanyName() != null)
                ? store.getSeller().getCompanyName()
                : (store != null ? store.getName() : "HinchMart B2B Commerce Pvt Ltd");
        String sellerGstin = (store != null && store.getSeller() != null && store.getSeller().getGstin() != null)
                ? store.getSeller().getGstin() : "36AAACH2026Q1Z1";

        List<InvoiceResponse.InvoiceItem> invoiceItems = order.getItems().stream()
                .map(item -> InvoiceResponse.InvoiceItem.builder()
                        .itemId(item.getOrderItemId())
                        .description(item.getTitle())
                        .hsnCode("721420")
                        .quantity(item.getQuantity())
                        .unit(item.getUnit())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .gstRate(item.getGstRate())
                        .gstAmount(item.getLineGst())
                        .build())
                .toList();

        String invNum = (order.getStoreInvoiceNumber() != null && !order.getStoreInvoiceNumber().isBlank())
                ? order.getStoreInvoiceNumber() : "INV-" + String.format("%06d", order.getOrderId());

        return InvoiceResponse.builder()
                .invoiceNumber(invNum)
                .invoiceDate(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")))
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .supplierName(sellerCompanyName)
                .supplierGstin(sellerGstin)
                .supplierAddress("HITEC City, Hyderabad, Telangana - 500081")
                .recipientName(customer != null && customer.getName() != null ? customer.getName() : "Enterprise Customer")
                .recipientAddress(order.getDeliveryLocation())
                .recipientGstin("36AAACT2727Q1ZW")
                .placeOfSupply("Telangana (36)")
                .paymentMethod(order.getPaymentMethod())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .taxableAmount(order.getTaxableAmount())
                .cgst(order.getCgst())
                .sgst(order.getSgst())
                .igst(order.getIgst())
                .totalGst(order.getTotalGst())
                .freightCharge(order.getFreightCharge())
                .craneUnloadingCharge(order.getCraneUnloadingCharge())
                .grandTotal(order.getTotalAmount())
                .items(invoiceItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Integer id) {
        Order order = findOrder(id);
        validateOrderReadAccess(order);
        Customer customer = order.getCustomer();
        String invoiceNumber = (order.getStoreInvoiceNumber() != null && !order.getStoreInvoiceNumber().isBlank())
                ? order.getStoreInvoiceNumber() : "INV-" + String.format("%06d", order.getOrderId());
        return pdfInvoiceGeneratorService.generateInvoicePdf(order, customer, invoiceNumber);
    }

    @Override
    public OrderResponse updateOrderStatus(Integer id, String status, String location, String description) {
        Order order = findOrder(id);
        validateOrderStatusUpdateAccess(order);
        String upperStatus = status.trim().toUpperCase();
        order.setOrderStatus(upperStatus);

        TrackingCheckpoint cp = TrackingCheckpoint.builder()
                .order(order)
                .status(upperStatus)
                .title(formatCheckpointTitle(upperStatus))
                .location(location != null ? location : "In Transit Hub")
                .description(description != null ? description : "Status updated to " + upperStatus)
                .timestamp(LocalDateTime.now())
                .build();

        order.getCheckpoints().add(cp);
        Order updated = orderRepository.save(order);

        if ("DELIVERED".equalsIgnoreCase(upperStatus)) {
            sellerPayoutLedgerRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(ledger -> {
                ledger.setStatus(PayoutLedgerStatus.PENDING);
                ledger.setSettlementDate(LocalDateTime.now().plusDays(7)); // T+7 days settlement
                sellerPayoutLedgerRepository.save(ledger);
            });
        }

        return mapToOrderResponse(updated);
    }

    @Override
    public OrderResponse cancelOrder(Integer id, String location, String description) {
        Order order = findOrder(id);
        validateOrderCancelAccess(order);
        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Delivered orders cannot be cancelled directly. Please raise a return/dispute.");
        }

        order.setOrderStatus("CANCELLED");
        order.setPaymentStatus("REFUND_PENDING");

        TrackingCheckpoint cp = TrackingCheckpoint.builder()
                .order(order)
                .status("CANCELLED")
                .title("Order Cancelled")
                .location(location != null ? location : "Customer Request")
                .description(description != null ? description : "Order cancelled before dispatch.")
                .timestamp(LocalDateTime.now())
                .build();

        order.getCheckpoints().add(cp);
        Order updated = orderRepository.save(order);

        // Restore stock
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                restoreStock(item);
            }
        }

        // Mark payout ledger as REVERSED
        sellerPayoutLedgerRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(ledger -> {
            ledger.setStatus(PayoutLedgerStatus.REVERSED);
            ledger.setClawbackReason("Pre-dispatch order cancellation by customer");
            sellerPayoutLedgerRepository.save(ledger);
            log.info("Payout ledger for Order #{} marked as REVERSED", order.getOrderId());
        });

        return mapToOrderResponse(updated);
    }

    private void validateOrderReadAccess(Order order) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Integer currentUserId = userContextUtil.getOptionalCurrentUserId();
        if (currentUserId != null && order.getCustomer() != null && currentUserId.equals(order.getCustomer().getCustomerId())) {
            return;
        }
        // Check if current user is the seller who owns the store of this order
        Optional<Integer> currentSellerId = SecurityUtils.getCurrentSellerId();
        if (currentSellerId.isEmpty()) {
            try {
                currentSellerId = Optional.ofNullable(sellerContextUtil.getCurrentSellerId());
            } catch (Exception ignored) {}
        }
        if (currentSellerId.isPresent() && order.getStore() != null && order.getStore().getSeller() != null
                && currentSellerId.get().equals(order.getStore().getSeller().getSellerId())) {
            return;
        }
        throw new ForbiddenException("Access denied: You do not have permission to access Order #" + order.getOrderId());
    }

    private void validateOrderCancelAccess(Order order) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Integer currentUserId = userContextUtil.getCurrentUserId();
        if (order.getCustomer() != null && currentUserId.equals(order.getCustomer().getCustomerId())) {
            return;
        }
        throw new ForbiddenException("Access denied: You can only cancel your own orders.");
    }

    private void validateOrderStatusUpdateAccess(Order order) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (SecurityUtils.isSeller()) {
            Optional<Integer> currentSellerId = SecurityUtils.getCurrentSellerId();
            if (currentSellerId.isEmpty()) {
                try {
                    currentSellerId = Optional.ofNullable(sellerContextUtil.getCurrentSellerId());
                } catch (Exception ignored) {}
            }
            if (currentSellerId.isPresent() && order.getStore() != null && order.getStore().getSeller() != null
                    && currentSellerId.get().equals(order.getStore().getSeller().getSellerId())) {
                return;
            }
        }
        throw new ForbiddenException("Access denied: Only administrators or the store owner can update order status.");
    }

    private synchronized void restoreStock(OrderItem orderItem) {
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

        Store store = o.getStore();
        return OrderSummaryResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .storeId(store != null ? store.getStoreId() : null)
                .storeName(store != null ? store.getName() : null)
                .storeSlug(store != null ? store.getSlug() : null)
                .storeInvoiceNumber(o.getStoreInvoiceNumber())
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
        Store store = o.getStore();

        return OrderResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .storeId(store != null ? store.getStoreId() : null)
                .storeName(store != null ? store.getName() : null)
                .storeSlug(store != null ? store.getSlug() : null)
                .storeInvoiceNumber(o.getStoreInvoiceNumber())
                .commissionRate(o.getCommissionRate())
                .commissionAmount(o.getCommissionAmount())
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
