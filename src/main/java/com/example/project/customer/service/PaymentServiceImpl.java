package com.example.project.customer.service;

import com.example.project.customer.dto.PaymentOrderCreateRequest;
import com.example.project.customer.dto.PaymentOrderCreateResponse;
import com.example.project.customer.dto.PaymentStatusResponse;
import com.example.project.customer.dto.PaymentVerifyRequest;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.Payment;
import com.example.project.customer.entity.Wallet;
import com.example.project.customer.entity.WalletTransaction;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.PaymentRepository;
import com.example.project.customer.repository.WalletRepository;
import com.example.project.customer.repository.WalletTransactionRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.example.project.customer.event.OrderCancelledEvent;
import org.springframework.context.event.EventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.project.customer.security.SecurityUtils;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.repository.AddressRepository;

import com.example.project.customer.service.outbox.OutboxService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final AddressRepository addressRepository;
    private final UserContextUtil userContextUtil;
    private final OutboxService outboxService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.example.project.customer.service.helper.PaymentRefundExecutionHelper paymentRefundExecutionHelper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private PaymentService self;

    private int doClaimRefundAtomic(Integer paymentId) {
        if (paymentRefundExecutionHelper != null) {
            return paymentRefundExecutionHelper.claimRefundAtomic(paymentId);
        }
        return paymentRepository.claimRefundAtomic(paymentId);
    }

    private void doRecordRefundStatus(Payment payment, String status, String errorDescription) {
        if (payment == null) {
            return;
        }
        payment.setStatus(status);
        if (errorDescription != null) {
            payment.setErrorDescription(errorDescription);
        }
        if (paymentRefundExecutionHelper != null && payment.getPaymentId() != null) {
            paymentRefundExecutionHelper.recordRefundStatus(payment.getPaymentId(), status, errorDescription);
        }
        paymentRepository.save(payment);
    }

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    @Override
    public PaymentOrderCreateResponse createPaymentOrder(Integer customerId, PaymentOrderCreateRequest request) {
        if (customerId == null) {
            throw new UnauthorizedException("Authentication required: Customer ID must not be null.");
        }
        int uid = customerId;
        Customer customer = customerRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + uid));

        Order order = null;
        BigDecimal amount = request.getAmount();
        String receipt = "RCP-" + System.currentTimeMillis();
        String purpose = request.getPurpose() != null ? request.getPurpose().toUpperCase() : "ORDER_PAYMENT";
        String description = "Payment for Construction Materials";

        // 1. If paying for an existing Order -> Fetch price directly from Order
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));
            amount = order.getTotalAmount();
            receipt = order.getOrderNumber() != null ? order.getOrderNumber() : "ORD-" + order.getOrderId();
            purpose = "ORDER_PAYMENT";
            description = "Payment for Order #"
                    + (order.getOrderNumber() != null ? order.getOrderNumber() : order.getOrderId());
        }
        // 2. If paying directly from Cart or Checkout preview (or if amount is not
        // specified and purpose is not WALLET_TOPUP)
        else if ("CART".equalsIgnoreCase(purpose) || "CART_PAYMENT".equalsIgnoreCase(purpose)
                || "CHECKOUT".equalsIgnoreCase(purpose)
                || (amount == null && !"WALLET_TOPUP".equalsIgnoreCase(purpose))) {
            CartResponse cart = cartService.getCart(uid);
            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                throw new IllegalStateException(
                        "Cannot generate payment for an empty cart. Please add items to your cart first.");
            }

            if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                amount = request.getAmount();
                purpose = "CHECKOUT";
                description = "Checkout Payment for " + cart.getItems().size() + " items (Amount: ₹" + amount + ")";
            } else if (request.getAddressId() != null) {
                CheckoutPreviewRequest previewReq = CheckoutPreviewRequest.builder()
                        .addressId(request.getAddressId())
                        .deliverySlot(request.getDeliverySlot())
                        .requiresCraneUnloading(request.getRequiresCraneUnloading())
                        .build();
                CheckoutPreviewResponse preview = checkoutService.previewCheckout(uid, previewReq);
                amount = preview.getGrandTotal();
                purpose = "CHECKOUT";
                description = "Checkout Payment for " + cart.getItems().size() + " items (incl. GST & Freight)";
            } else {
                amount = cart.getGrandTotal() != null ? cart.getGrandTotal() : cart.getSubtotal();
                purpose = "CART_PAYMENT";
                description = "Cart Payment for " + cart.getItems().size() + " items (Total: ₹" + amount + ")";
            }
            receipt = "CRT-" + uid + "-" + System.currentTimeMillis();
        }
        // 3. If Wallet Top-up
        else if ("WALLET_TOPUP".equalsIgnoreCase(purpose)) {
            if (amount == null || amount.compareTo(BigDecimal.ONE) < 0) {
                throw new IllegalArgumentException("Wallet top-up amount must be at least ₹1.00");
            }
            receipt = "WLT-" + uid + "-" + System.currentTimeMillis();
            description = "Payment for Wallet Top-up";
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        // Amount in paise (1 INR = 100 paise)
        long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();

        try {
            JSONObject rzpOptions = new JSONObject();
            rzpOptions.put("amount", amountInPaise);
            rzpOptions.put("currency", "INR");
            rzpOptions.put("receipt", receipt);

            JSONObject notes = new JSONObject();
            notes.put("customerId", String.valueOf(uid));
            notes.put("purpose", purpose);
            if (order != null) {
                notes.put("orderId", String.valueOf(order.getOrderId()));
            }
            if (request.getAddressId() != null) {
                notes.put("addressId", String.valueOf(request.getAddressId()));
            }
            if (request.getDeliverySlot() != null) {
                notes.put("deliverySlot", request.getDeliverySlot());
            }
            if (request.getRequiresCraneUnloading() != null) {
                notes.put("requiresCraneUnloading", String.valueOf(request.getRequiresCraneUnloading()));
            }
            if (Boolean.TRUE.equals(request.getOrderForSomeoneElse())) {
                notes.put("orderForSomeoneElse", "true");
            }
            if (request.getRecipientName() != null && !request.getRecipientName().isBlank()) {
                notes.put("recipientName", request.getRecipientName().trim());
            }
            if (request.getRecipientPhone() != null && !request.getRecipientPhone().isBlank()) {
                notes.put("recipientPhone", request.getRecipientPhone().trim());
            }
            rzpOptions.put("notes", notes);

            com.razorpay.Order rzpOrder = null;
            RazorpayException lastException = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    rzpOrder = razorpayClient.orders.create(rzpOptions);
                    break;
                } catch (RazorpayException e) {
                    lastException = e;
                    log.warn("Attempt {} to connect to Razorpay failed: {}", attempt, e.getMessage());
                    if (attempt < 3) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }

            if (rzpOrder == null) {
                if (lastException != null && lastException.getMessage() != null
                        && lastException.getMessage().contains("No such host is known")) {
                    throw new RuntimeException(
                            "Unable to connect to Razorpay (Network/DNS error). Please check your internet connection and try again.");
                }
                throw lastException;
            }

            String razorpayOrderId = rzpOrder.get("id");

            // Persist payment attempt in database
            Payment payment = Payment.builder()
                    .customer(customer)
                    .order(order)
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amount)
                    .currency("INR")
                    .status("CREATED")
                    .purpose(purpose)
                    .build();
            paymentRepository.save(payment);

            log.info("Created Razorpay order {} for customer {} with amount {} INR (purpose: {})", razorpayOrderId, uid,
                    amount, purpose);

            return PaymentOrderCreateResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .keyId(keyId)
                    .amount(amount)
                    .amountInPaise(amountInPaise)
                    .currency("INR")
                    .orderId(order != null ? order.getOrderId() : null)
                    .orderNumber(order != null ? order.getOrderNumber() : null)
                    .purpose(purpose)
                    .customerName(customer.getName())
                    .customerEmail(customer.getEmail())
                    .customerPhone(customer.getPhone())
                    .description(description)
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Razorpay order creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public PaymentStatusResponse verifyPayment(Integer customerId, PaymentVerifyRequest request) {
        log.info("Verifying Razorpay payment for orderId: {}, paymentId: {}", request.getOrderId(),
                request.getRazorpayPaymentId());

        // Step 1: Cryptographic signature verification using HMAC-SHA256
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(attributes, keySecret);
            if (!isValid) {
                log.warn("Invalid payment signature received for razorpay_payment_id: {}",
                        request.getRazorpayPaymentId());
                throw new IllegalArgumentException("Invalid Razorpay payment signature.");
            }
        } catch (RazorpayException e) {
            log.error("Error during signature verification: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Payment signature verification failed: " + e.getMessage());
        }

        // Step 2: Fetch live payment status directly from Razorpay
        com.razorpay.Payment rzpPayment = null;
        RazorpayException lastFetchEx = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                rzpPayment = razorpayClient.payments.fetch(request.getRazorpayPaymentId());
                break;
            } catch (RazorpayException e) {
                lastFetchEx = e;
                log.warn("Attempt {} to fetch Razorpay payment {} failed: {}", attempt, request.getRazorpayPaymentId(),
                        e.getMessage());
                if (attempt < 3) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        if (rzpPayment == null) {
            log.error("Failed to fetch payment {} from Razorpay after retries: {}", request.getRazorpayPaymentId(),
                    lastFetchEx != null ? lastFetchEx.getMessage() : "Unknown error");
            throw new RuntimeException("Failed to verify payment with Razorpay gateway (network error): "
                    + (lastFetchEx != null ? lastFetchEx.getMessage() : ""), lastFetchEx);
        }

        String rzpStatus = rzpPayment.get("status"); // "captured", "authorized", "failed"
        log.info("Razorpay payment {} status: {}", request.getRazorpayPaymentId(), rzpStatus);

        BigDecimal verifiedAmount = BigDecimal.valueOf(((Number) rzpPayment.get("amount")).doubleValue() / 100.0);

        // Step 3: Find or update local Payment record
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseGet(() -> {
                    Customer c = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
                    Order o = request.getOrderId() != null ? orderRepository.findById(request.getOrderId()).orElse(null)
                            : null;
                    return Payment.builder()
                            .customer(c)
                            .order(o)
                            .razorpayOrderId(request.getRazorpayOrderId())
                            .amount(verifiedAmount)
                            .currency("INR")
                            .purpose("ORDER_PAYMENT")
                            .build();
                });

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        if (!isRefundOrPendingState(payment.getStatus())) {
            payment.setStatus(rzpStatus != null ? rzpStatus.toUpperCase() : "CAPTURED");
        }

        if (rzpPayment.has("method") && rzpPayment.get("method") != null) {
            payment.setPaymentMethod(rzpPayment.get("method").toString().toUpperCase());
        }
        if (rzpPayment.has("error_code") && rzpPayment.get("error_code") != null) {
            payment.setErrorCode(rzpPayment.get("error_code").toString());
        }
        if (rzpPayment.has("error_description") && rzpPayment.get("error_description") != null) {
            payment.setErrorDescription(rzpPayment.get("error_description").toString());
        }
        paymentRepository.save(payment);

        if (isRefundOrPendingState(payment.getStatus())) {
            log.warn("Payment #{} is in refund state ({}); skipping order confirmation and preserving refund status.",
                    payment.getPaymentId(), payment.getStatus());
            return mapToStatusResponse(payment, rzpPayment);
        }

        // Step 4: If payment is captured/authorized, update business entities (Order or
        // Wallet)
        if ("captured".equalsIgnoreCase(rzpStatus) || "authorized".equalsIgnoreCase(rzpStatus)) {
            // Update Order
            Order order = payment.getOrder();
            if (order == null && request.getOrderId() != null) {
                order = orderRepository.findById(request.getOrderId()).orElse(null);
            }

            // If paying from CART_PAYMENT or CHECKOUT without an existing order,
            // automatically convert cart into placed order
            if (order == null && ("CART_PAYMENT".equalsIgnoreCase(payment.getPurpose())
                    || "CHECKOUT".equalsIgnoreCase(payment.getPurpose()))) {
                try {
                    Integer addrId = null;
                    if (rzpPayment.has("notes") && rzpPayment.get("notes") != null) {
                        Object nObj = rzpPayment.get("notes");
                        if (nObj instanceof JSONObject nJson && nJson.has("addressId")) {
                            addrId = Integer.parseInt(nJson.optString("addressId"));
                        }
                    }
                    if (addrId == null && payment.getCustomer() != null) {
                        Address defAddr = addressRepository
                                .findByCustomer_CustomerIdAndIsDefaultTrue(payment.getCustomer().getCustomerId())
                                .orElse(null);
                        if (defAddr != null)
                            addrId = defAddr.getId();
                    }

                    if (addrId != null) {
                        OrderCreateRequest oReq = OrderCreateRequest.builder()
                                .addressId(addrId)
                                .paymentMethod(
                                        payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "RAZORPAY")
                                .deliveryInstructions(
                                        "Auto-placed from Razorpay Payment " + payment.getRazorpayPaymentId())
                                .build();
                        OrderResponse placed = orderService.createOrder(payment.getCustomer().getCustomerId(), oReq);
                        order = orderRepository.findById(placed.getOrderId()).orElse(null);
                        payment.setOrder(order);
                        paymentRepository.save(payment);
                        log.info("Auto-placed Order #{} for customer #{} from captured payment {}",
                                placed.getOrderId(), payment.getCustomer().getCustomerId(),
                                payment.getRazorpayPaymentId());
                    } else {
                        throw new IllegalStateException(
                                "No delivery address available to auto-place order from cart payment.");
                    }
                } catch (Exception e) {
                    log.error("Failed to auto-create order from payment {}: {}", payment.getRazorpayPaymentId(),
                            e.getMessage(), e);
                    refundPaymentOrCreditWallet(payment, payment.getRazorpayPaymentId(),
                            "Order auto-placement failed: " + e.getMessage());
                }
            }

            if (order != null) {
                // If payment captured AFTER order was cancelled or expired -> auto-refund
                if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus()) || "EXPIRED".equalsIgnoreCase(order.getPaymentStatus())) {
                    log.warn("Payment {} captured for already cancelled/expired Order #{}. Initiating automatic refund.",
                            request.getRazorpayPaymentId(), order.getOrderId());
                    refundPaymentOrCreditWallet(payment, request.getRazorpayPaymentId(),
                            "Late payment captured for cancelled/expired order #" + order.getOrderId());
                    return mapToStatusResponse(payment, rzpPayment);
                }

                order.setPaymentStatus("PAID");
                if (payment.getPaymentMethod() != null) {
                    order.setPaymentMethod(payment.getPaymentMethod());
                }
                boolean transitioningToConfirmed = "PLACED".equalsIgnoreCase(order.getOrderStatus())
                        || "PENDING".equalsIgnoreCase(order.getOrderStatus());
                if (transitioningToConfirmed) {
                    order.setOrderStatus("CONFIRMED");
                    orderService.commitHinchStoreReservation(order);
                }
                orderRepository.save(order);
                log.info("Order #{} marked as PAID and CONFIRMED", order.getOrderId());
                if (transitioningToConfirmed) {
                    outboxService.recordOrderConfirmed(order);
                }
            }

            // Update Wallet if purpose is WALLET_TOPUP
            if ("WALLET_TOPUP".equalsIgnoreCase(payment.getPurpose()) && payment.getCustomer() != null) {
                creditCustomerWallet(payment.getCustomer().getCustomerId(), payment.getAmount(),
                        payment.getRazorpayPaymentId());
            }
        }

        return mapToStatusResponse(payment, rzpPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String razorpayPaymentId) {
        try {
            com.razorpay.Payment rzpPayment = razorpayClient.payments.fetch(razorpayPaymentId);
            Payment localPayment = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId).orElse(null);
            return mapToStatusResponse(localPayment, rzpPayment);
        } catch (RazorpayException e) {
            log.error("Failed to fetch payment status for {}: {}", razorpayPaymentId, e.getMessage());
            throw new ResourceNotFoundException("Razorpay payment not found: " + razorpayPaymentId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getLatestOrderPaymentStatus(Integer orderId) {
        Payment payment = paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment record found for order id: " + orderId));

        if (!SecurityUtils.isAdmin()) {
            Integer currentUserId = userContextUtil.getCurrentUserId();
            if (currentUserId == null) {
                throw new UnauthorizedException("Authentication required: Please log in to view order payment status.");
            }
            Integer orderBuyerId = (payment.getOrder() != null && payment.getOrder().getCustomer() != null)
                    ? payment.getOrder().getCustomer().getCustomerId()
                    : (payment.getCustomer() != null ? payment.getCustomer().getCustomerId() : null);
            if (orderBuyerId == null || !orderBuyerId.equals(currentUserId)) {
                throw new ForbiddenException("Access denied: You do not own this order's payment.");
            }
        }

        Customer cust = payment.getCustomer();
        Integer custId = cust != null ? cust.getCustomerId() : null;
        String custName = cust != null ? cust.getName() : null;
        String custPhone = cust != null ? cust.getPhone() : null;

        return PaymentStatusResponse.builder()
                .paymentId(payment.getPaymentId())
                .customerId(custId)
                .customerName(custName)
                .customerPhone(custPhone)
                .orderId(payment.getOrder() != null ? payment.getOrder().getOrderId() : null)
                .orderNumber(payment.getOrder() != null ? payment.getOrder().getOrderNumber() : null)
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .purpose(payment.getPurpose())
                .paymentMethod(payment.getPaymentMethod())
                .errorCode(payment.getErrorCode())
                .errorDescription(payment.getErrorDescription())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentStatusResponse> getCustomerPayments(Integer customerId) {
        if (customerId == null) {
            throw new UnauthorizedException("Authentication required: Customer ID must not be null.");
        }
        if (!SecurityUtils.isAdmin()) {
            Integer currentUserId = userContextUtil.getCurrentUserId();
            if (currentUserId == null) {
                throw new UnauthorizedException("Authentication required: Please log in to view payment history.");
            }
            if (!currentUserId.equals(customerId)) {
                throw new ForbiddenException("Access denied: You can only view your own payment records.");
            }
        }
        int uid = customerId;
        return paymentRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(uid).stream()
                .map(p -> mapToStatusResponse(p, null))
                .toList();
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        log.info("Received Razorpay webhook event");

        if (webhookSecret == null || webhookSecret.isBlank() || "placeholder_webhook_secret".equals(webhookSecret)) {
            log.error("Razorpay webhook secret is not configured on the server. Rejecting webhook request.");
            throw new IllegalStateException("Razorpay webhook secret is not configured on the server.");
        }

        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Missing X-Razorpay-Signature header in webhook request");
            throw new IllegalArgumentException("Missing X-Razorpay-Signature header.");
        }

        try {
            boolean valid = Utils.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
            if (!valid) {
                log.warn("Invalid Razorpay webhook signature");
                throw new IllegalArgumentException("Invalid webhook signature");
            }
        } catch (RazorpayException e) {
            log.error("Webhook signature check error: {}", e.getMessage());
            throw new IllegalArgumentException("Webhook verification failed: " + e.getMessage());
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.optString("event");
        log.info("Processing Razorpay webhook event: {}", eventType);

        if ("payment.captured".equals(eventType) || "order.paid".equals(eventType)) {
            JSONObject payloadObj = event.getJSONObject("payload");
            JSONObject paymentObj = payloadObj.getJSONObject("payment").getJSONObject("entity");

            String rzpPaymentId = paymentObj.getString("id");
            String rzpOrderId = paymentObj.getString("order_id");
            String method = paymentObj.optString("method");

            paymentRepository.findByRazorpayOrderId(rzpOrderId).ifPresent(p -> {
                if (isRefundOrPendingState(p.getStatus())) {
                    log.warn("Webhook captured event for payment #{} which is in refund state ({}); skipping status overwrite to CAPTURED.",
                            p.getPaymentId(), p.getStatus());
                    return;
                }
                p.setRazorpayPaymentId(rzpPaymentId);
                p.setStatus("CAPTURED");
                if (method != null && !method.isBlank()) {
                    p.setPaymentMethod(method.toUpperCase());
                }
                paymentRepository.save(p);

                // Branch by purpose: WALLET_TOPUP vs Order Payment
                if ("WALLET_TOPUP".equalsIgnoreCase(p.getPurpose()) && p.getCustomer() != null) {
                    creditCustomerWallet(p.getCustomer().getCustomerId(), p.getAmount(), rzpPaymentId);
                } else if (p.getOrder() != null) {
                    Order o = p.getOrder();
                    if ("CANCELLED".equalsIgnoreCase(o.getOrderStatus()) || "EXPIRED".equalsIgnoreCase(o.getPaymentStatus())) {
                        log.warn("Late payment received via webhook for already cancelled/expired Order #{}. Initiating automatic refund.", o.getOrderId());
                        refundPaymentOrCreditWallet(p, rzpPaymentId, "Late payment captured via webhook for cancelled/expired order #" + o.getOrderId());
                        return;
                    }
                    o.setPaymentStatus("PAID");
                    if (p.getPaymentMethod() != null) {
                        o.setPaymentMethod(p.getPaymentMethod());
                    }
                    boolean transitioningToConfirmed = "PLACED".equalsIgnoreCase(o.getOrderStatus())
                            || "PENDING".equalsIgnoreCase(o.getOrderStatus());
                    if (transitioningToConfirmed) {
                        o.setOrderStatus("CONFIRMED");
                        orderService.commitHinchStoreReservation(o);
                    }
                    orderRepository.save(o);
                    log.info("Order #{} marked PAID via webhook", o.getOrderId());
                    if (transitioningToConfirmed) {
                        outboxService.recordOrderConfirmed(o);
                    }
                } else if ("CART_PAYMENT".equalsIgnoreCase(p.getPurpose())
                        || "CHECKOUT".equalsIgnoreCase(p.getPurpose())) {
                    // Auto-create order from webhook if verifyPayment was never called (e.g. mobile
                    // app crash or network drop)
                    try {
                        Integer addrId = null;
                        if (paymentObj.has("notes") && paymentObj.get("notes") != null) {
                            Object nObj = paymentObj.get("notes");
                            if (nObj instanceof JSONObject nJson && nJson.has("addressId")) {
                                addrId = Integer.parseInt(nJson.optString("addressId"));
                            }
                        }
                        if (addrId == null && p.getCustomer() != null) {
                            Address defAddr = addressRepository
                                    .findByCustomer_CustomerIdAndIsDefaultTrue(p.getCustomer().getCustomerId())
                                    .orElse(null);
                            if (defAddr != null)
                                addrId = defAddr.getId();
                        }

                        if (addrId != null && p.getCustomer() != null) {
                            OrderCreateRequest oReq = OrderCreateRequest.builder()
                                    .addressId(addrId)
                                    .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod() : "RAZORPAY")
                                    .deliveryInstructions("Auto-placed from Razorpay Webhook " + rzpPaymentId)
                                    .build();
                            OrderResponse placed = orderService.createOrder(p.getCustomer().getCustomerId(), oReq);
                            Order order = orderRepository.findById(placed.getOrderId()).orElse(null);
                            if (order != null) {
                                order.setPaymentStatus("PAID");
                                order.setOrderStatus("CONFIRMED");
                                orderRepository.save(order);
                                p.setOrder(order);
                                paymentRepository.save(p);
                                outboxService.recordOrderConfirmed(order);
                                log.info("Auto-placed Order #{} via webhook for customer #{} from captured payment {}",
                                        order.getOrderId(), p.getCustomer().getCustomerId(), rzpPaymentId);
                            }
                        } else {
                            throw new IllegalStateException(
                                    "No delivery address available to auto-place order from webhook.");
                        }
                    } catch (Exception e) {
                        log.error("Failed to auto-create order from webhook for payment {}: {}", rzpPaymentId,
                                e.getMessage(), e);
                        refundPaymentOrCreditWallet(p, rzpPaymentId,
                                "Webhook auto-placement failed: " + e.getMessage());
                    }
                }
            });
        } else if ("payment.failed".equals(eventType)) {
            JSONObject payloadObj = event.getJSONObject("payload");
            JSONObject paymentObj = payloadObj.getJSONObject("payment").getJSONObject("entity");

            String rzpOrderId = paymentObj.optString("order_id");
            paymentRepository.findByRazorpayOrderId(rzpOrderId).ifPresent(p -> {
                if (isRefundOrPendingState(p.getStatus())) {
                    log.warn("Webhook failure received for payment #{} in refund state ({}); skipping status overwrite to FAILED.",
                            p.getPaymentId(), p.getStatus());
                    return;
                }
                p.setStatus("FAILED");
                p.setErrorCode(paymentObj.optString("error_code"));
                p.setErrorDescription(paymentObj.optString("error_description"));
                paymentRepository.save(p);

                if (p.getOrder() != null) {
                    p.getOrder().setPaymentStatus("FAILED");
                    orderRepository.save(p.getOrder());
                }
            });
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Handling OrderCancelledEvent for Order #{}. Initiating automatic refund...", event.orderId());
        try {
            PaymentService service = (self != null) ? self : this;
            service.refundOrderPayment(event.orderId(), event.reason());
        } catch (Exception e) {
            log.error("Failed to process auto-refund for cancelled Order #{}: {}", event.orderId(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentStatusResponse refundOrderPayment(Integer orderId, String reason) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null for refund processing.");
        }

        Payment payment = paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(orderId).orElse(null);
        Order order = (payment != null && payment.getOrder() != null)
                ? payment.getOrder()
                : orderRepository.findById(orderId).orElse(null);

        if (order != null) {
            validateOrderRefundAccess(order, payment);
        }

        if (payment == null) {
            log.warn("No payment record found for Order #{}. Marking paymentStatus as REFUNDED directly.", orderId);
            if (order != null) {
                order.setPaymentStatus("REFUNDED");
                orderRepository.save(order);
            }
            return PaymentStatusResponse.builder()
                    .orderId(orderId)
                    .status("NO_PAYMENT_RECORD")
                    .errorDescription("No payment record found to refund for order " + orderId)
                    .build();
        }

        if ("REFUNDED".equalsIgnoreCase(payment.getStatus())
                || "REFUNDED_TO_WALLET".equalsIgnoreCase(payment.getStatus())) {
            log.info("Payment #{} for Order #{} is already refunded: {}", payment.getPaymentId(), orderId,
                    payment.getStatus());
            return mapToStatusResponse(payment, null);
        }

        refundPaymentOrCreditWallet(payment, payment.getRazorpayPaymentId(),
                reason != null ? reason : "Order #" + orderId + " cancellation refund");

        if (order != null) {
            if ("REFUNDED".equalsIgnoreCase(payment.getStatus()) || "REFUNDED_TO_WALLET".equalsIgnoreCase(payment.getStatus())) {
                order.setPaymentStatus("REFUNDED");
            } else if ("REFUND_PENDING".equalsIgnoreCase(payment.getStatus())) {
                order.setPaymentStatus("REFUND_PENDING");
            }
            orderRepository.save(order);
        }

        return mapToStatusResponse(payment, null);
    }

    private void validateOrderRefundAccess(Order order, Payment payment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            // Internal call (event listener, background task, system execution)
            return;
        }

        if (SecurityUtils.isAdmin()) {
            return;
        }

        Integer currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId == null && userContextUtil != null) {
            try {
                currentUserId = userContextUtil.getOptionalCurrentUserId();
            } catch (Exception ignored) {}
        }

        if (currentUserId != null && order != null && order.getCustomer() != null && currentUserId.equals(order.getCustomer().getCustomerId())) {
            String status = order.getOrderStatus();
            boolean isAllowedOrderState = "CANCELLED".equalsIgnoreCase(status)
                    || "RETURNED".equalsIgnoreCase(status);
            if (!isAllowedOrderState) {
                throw new IllegalStateException("Refund can only be requested for cancelled or returned orders.");
            }

            if (payment != null) {
                String paymentStatus = payment.getStatus();
                if ("REFUND_PENDING".equalsIgnoreCase(paymentStatus) || "REFUND_IN_PROGRESS".equalsIgnoreCase(paymentStatus)) {
                    throw new ForbiddenException("Retrying pending or in-progress refunds is restricted to administrators.");
                }
                if (!"CAPTURED".equalsIgnoreCase(paymentStatus)) {
                    throw new IllegalStateException("Refund can only be requested for captured payments (current status: " + paymentStatus + ").");
                }
            }
            return;
        }

        throw new ForbiddenException("Access denied: You can only refund your own orders.");
    }

    /**
     * Customer Fund Protection:
     * When payment is captured by Razorpay, but the corresponding order cannot be
     * fulfilled
     * (e.g. out of stock, inactive store, validation failure) or was cancelled,
     * ensures the customer never loses money.
     * 1. Attempts immediate online refund via Razorpay refund API.
     * 2. If Razorpay refund fails or is delayed, credits 100% of the funds to the
     * customer's in-app wallet.
     */
    private void refundPaymentOrCreditWallet(Payment payment, String razorpayPaymentId, String reason) {
        if (payment == null) {
            return;
        }
        // Ensure entity is persisted so paymentId is available
        if (payment.getPaymentId() == null) {
            payment = paymentRepository.save(payment);
        }

        // (a) Claim the refund atomically in an isolated transaction so it survives rollbacks
        int claimed = doClaimRefundAtomic(payment.getPaymentId());
        if (claimed == 0) {
            log.info("Payment #{} refund already claimed or in final state (status: {}). Skipping duplicate refund attempt.",
                    payment.getPaymentId(), payment.getStatus());
            return;
        }
        payment.setStatus("REFUND_IN_PROGRESS");

        log.warn("Initiating customer protection refund for payment {} (Customer: #{}). Reason: {}",
                razorpayPaymentId, payment.getCustomer() != null ? payment.getCustomer().getCustomerId() : "unknown",
                reason);

        boolean refundedViaGateway = false;
        if (razorpayPaymentId != null && !razorpayPaymentId.isBlank()) {
            try {
                JSONObject refundRequest = new JSONObject();
                // (c) receipt and notes kept for human/gateway reference only (not relied upon as the idempotency mechanism)
                refundRequest.put("receipt", "ref_" + razorpayPaymentId.trim());
                refundRequest.put("notes", new JSONObject()
                        .put("reason", "Automatic refund")
                        .put("details", reason != null ? reason : "Customer protection refund"));
                com.razorpay.Refund rzpRefund = razorpayClient.payments.refund(razorpayPaymentId, refundRequest);
                String refundId = (rzpRefund != null && rzpRefund.has("id")) ? rzpRefund.get("id") : "processed";
                payment.setStatus("REFUNDED");
                payment.setErrorDescription(
                        "Payment automatically refunded via Razorpay (Refund ID: " + refundId + "). Reason: " + reason);
                doRecordRefundStatus(payment, "REFUNDED", payment.getErrorDescription());
                refundedViaGateway = true;
                log.info("Successfully processed gateway refund {} for payment {}", refundId, razorpayPaymentId);
            } catch (Exception rzpEx) {
                log.error("Razorpay online refund threw exception for payment {}: {}. Inspecting existing refunds from gateway...",
                        razorpayPaymentId, rzpEx.getMessage(), rzpEx);
                // (b) on a Razorpay exception, fetch the payment's refunds from Razorpay first:
                // if one exists mark REFUNDED and stop; otherwise mark REFUND_PENDING (or similar) for retry/alert; do NOT credit the wallet on a bare exception;
                try {
                    List<com.razorpay.Refund> existingRefunds = razorpayClient.payments.fetchAllRefunds(razorpayPaymentId);
                    if (existingRefunds != null && !existingRefunds.isEmpty()) {
                        com.razorpay.Refund existing = existingRefunds.get(0);
                        String refundId = (existing != null && existing.has("id")) ? existing.get("id") : "existing";
                        payment.setStatus("REFUNDED");
                        payment.setErrorDescription("Discovered existing gateway refund (Refund ID: " + refundId + "). Reason: " + reason);
                        doRecordRefundStatus(payment, "REFUNDED", payment.getErrorDescription());
                        refundedViaGateway = true;
                        log.info("Verified existing gateway refund {} for payment {}. Marked REFUNDED.", refundId, razorpayPaymentId);
                    } else {
                        payment.setStatus("REFUND_PENDING");
                        payment.setErrorDescription("Razorpay refund failed (" + rzpEx.getMessage() + ") and no gateway refund exists. Queued for operational review/retry.");
                        doRecordRefundStatus(payment, "REFUND_PENDING", payment.getErrorDescription());
                        log.warn("ALERT: Payment #{} refund failed at gateway ({}); marked REFUND_PENDING for retry/alert.", payment.getPaymentId(), rzpEx.getMessage());
                    }
                } catch (Exception fetchEx) {
                    log.error("ALERT: Failed to inspect refunds from Razorpay for payment {}: {}. Marked REFUND_PENDING.", razorpayPaymentId, fetchEx.getMessage(), fetchEx);
                    payment.setStatus("REFUND_PENDING");
                    payment.setErrorDescription("Razorpay refund exception (" + rzpEx.getMessage() + ") and status check failed. Marked REFUND_PENDING.");
                    doRecordRefundStatus(payment, "REFUND_PENDING", payment.getErrorDescription());
                }
            }
        }

        // Only offline or internal payments with no Razorpay payment ID credit wallet
        if (!refundedViaGateway && (razorpayPaymentId == null || razorpayPaymentId.isBlank()) && payment.getCustomer() != null) {
            try {
                creditCustomerWallet(
                        payment.getCustomer().getCustomerId(),
                        payment.getAmount(),
                        "REFUND-PAY-" + payment.getPaymentId(),
                        "REFUND",
                        "Automatic Wallet Refund: " + reason);
                payment.setStatus("REFUNDED_TO_WALLET");
                payment.setErrorDescription("Payment automatically credited to customer wallet. Reason: " + reason);
                paymentRepository.save(payment);
                log.info("Successfully credited {} INR to customer #{} wallet as internal refund",
                        payment.getAmount(), payment.getCustomer().getCustomerId());
            } catch (Exception walletEx) {
                log.error("CRITICAL: Failed to credit customer wallet for internal payment #{}: {}", payment.getPaymentId(),
                        walletEx.getMessage(), walletEx);
                payment.setStatus("REFUND_FAILED");
                payment.setErrorDescription("Auto-refund failed (" + reason + "). Immediate manual action required.");
                paymentRepository.save(payment);
            }
        }
    }

    private void creditCustomerWallet(Integer customerId, BigDecimal amount, String paymentId) {
        creditCustomerWallet(customerId, amount, paymentId, "TOPUP", "Wallet Top-up via Razorpay (" + paymentId + ")");
    }

    private synchronized void creditCustomerWallet(Integer customerId, BigDecimal amount, String paymentId,
            String source, String description) {
        if (customerId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid wallet credit parameters: customerId={}, amount={}", customerId, amount);
            return;
        }

        // Idempotency guard: prevent duplicate credit if this paymentId was already
        // processed
        if (paymentId != null && walletTransactionRepository.existsByReferenceId(paymentId)) {
            log.warn("Payment reference '{}' was already credited to customer {} wallet. Skipping duplicate credit.",
                    paymentId, customerId);
            return;
        }

        // Use pessimistic lock query to guarantee concurrency safety across multiple
        // pods
        Wallet wallet = walletRepository.findByCustomer_CustomerIdForUpdate(customerId)
                .orElseGet(() -> {
                    Customer c = customerRepository.findById(customerId).orElse(null);
                    Wallet w = Wallet.builder()
                            .customer(c)
                            .balance(BigDecimal.ZERO)
                            .currency("INR")
                            .loyaltyPoints(0)
                            .tier("STANDARD")
                            .active(true)
                            .build();
                    return walletRepository.save(w);
                });

        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .type("CREDIT")
                .amount(amount)
                .source(source != null ? source : "TOPUP")
                .referenceId(paymentId != null ? paymentId : "TXN-" + System.currentTimeMillis())
                .description(description != null ? description : "Wallet Credit (" + paymentId + ")")
                .balanceAfter(newBalance)
                .timestamp(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(txn);
        log.info("Credited wallet for customer {} with amount {} INR (source: {}). New balance: {}",
                customerId, amount, source, newBalance);
    }

    private PaymentStatusResponse mapToStatusResponse(Payment localPayment, com.razorpay.Payment rzpPayment) {
        String status = localPayment != null ? localPayment.getStatus() : "UNKNOWN";
        BigDecimal amount = localPayment != null ? localPayment.getAmount() : BigDecimal.ZERO;
        String method = localPayment != null ? localPayment.getPaymentMethod() : null;
        String email = null;
        String contact = null;
        String vpa = null;
        String bank = null;
        String cardNetwork = null;
        String cardLast4 = null;
        String errCode = localPayment != null ? localPayment.getErrorCode() : null;
        String errDesc = localPayment != null ? localPayment.getErrorDescription() : null;

        Integer customerId = null;
        String customerName = null;
        String customerPhone = null;

        if (localPayment != null && localPayment.getCustomer() != null) {
            Customer c = localPayment.getCustomer();
            customerId = c.getCustomerId();
            customerName = c.getName();
            customerPhone = c.getPhone();
            if (contact == null)
                contact = c.getPhone();
            if (email == null)
                email = c.getEmail();
        }

        if (rzpPayment != null) {
            if (rzpPayment.has("status") && rzpPayment.get("status") != null) {
                status = rzpPayment.get("status").toString().toUpperCase();
            }
            if (rzpPayment.has("amount") && rzpPayment.get("amount") != null) {
                amount = BigDecimal.valueOf(((Number) rzpPayment.get("amount")).doubleValue() / 100.0).setScale(2,
                        RoundingMode.HALF_UP);
            }
            if (rzpPayment.has("method") && rzpPayment.get("method") != null) {
                method = rzpPayment.get("method").toString().toUpperCase();
            }
            if (rzpPayment.has("email") && rzpPayment.get("email") != null) {
                email = rzpPayment.get("email").toString();
            }
            if (rzpPayment.has("contact") && rzpPayment.get("contact") != null) {
                contact = rzpPayment.get("contact").toString();
            }
            if (rzpPayment.has("vpa") && rzpPayment.get("vpa") != null) {
                vpa = rzpPayment.get("vpa").toString();
            }
            if (rzpPayment.has("bank") && rzpPayment.get("bank") != null) {
                bank = rzpPayment.get("bank").toString();
            }
            if (rzpPayment.has("card") && rzpPayment.get("card") != null) {
                Object cardObj = rzpPayment.get("card");
                if (cardObj instanceof JSONObject card) {
                    if (card.has("network") && !card.isNull("network"))
                        cardNetwork = card.optString("network");
                    if (card.has("last4") && !card.isNull("last4"))
                        cardLast4 = card.optString("last4");
                }
            }
            if (rzpPayment.has("error_code") && rzpPayment.get("error_code") != null) {
                errCode = rzpPayment.get("error_code").toString();
            }
            if (rzpPayment.has("error_description") && rzpPayment.get("error_description") != null) {
                errDesc = rzpPayment.get("error_description").toString();
            }
        }

        return PaymentStatusResponse.builder()
                .paymentId(localPayment != null ? localPayment.getPaymentId() : null)
                .customerId(customerId)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .orderId(localPayment != null && localPayment.getOrder() != null ? localPayment.getOrder().getOrderId()
                        : null)
                .orderNumber(localPayment != null && localPayment.getOrder() != null
                        ? localPayment.getOrder().getOrderNumber()
                        : null)
                .razorpayPaymentId(rzpPayment != null ? rzpPayment.get("id")
                        : (localPayment != null ? localPayment.getRazorpayPaymentId() : null))
                .razorpayOrderId(rzpPayment != null && rzpPayment.has("order_id") ? rzpPayment.get("order_id")
                        : (localPayment != null ? localPayment.getRazorpayOrderId() : null))
                .status(status != null ? status.toUpperCase() : "UNKNOWN")
                .amount(amount)
                .currency("INR")
                .purpose(localPayment != null ? localPayment.getPurpose() : null)
                .paymentMethod(method)
                .email(email)
                .contact(contact)
                .vpa(vpa)
                .bank(bank)
                .cardNetwork(cardNetwork)
                .cardLast4(cardLast4)
                .errorCode(errCode)
                .errorDescription(errDesc)
                .createdAt(localPayment != null ? localPayment.getCreatedAt() : LocalDateTime.now())
                .updatedAt(localPayment != null ? localPayment.getUpdatedAt() : LocalDateTime.now())
                .build();
    }

    private boolean isRefundOrPendingState(String status) {
        if (status == null) {
            return false;
        }
        String s = status.trim().toUpperCase();
        return "REFUND_IN_PROGRESS".equals(s)
                || "REFUND_PENDING".equals(s)
                || "REFUNDED".equals(s)
                || "REFUNDED_TO_WALLET".equals(s);
    }

    @Override
    @Transactional
    public int reconcileStaleRefunds(int olderThanMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(olderThanMinutes);
        List<Payment> stalePayments = paymentRepository.findByStatusAndUpdatedAtBefore("REFUND_IN_PROGRESS", cutoff);
        if (stalePayments.isEmpty()) {
            return 0;
        }

        log.info("Found {} stale REFUND_IN_PROGRESS payments older than {} minutes. Reconciling with Razorpay...",
                stalePayments.size(), olderThanMinutes);

        int reconciledCount = 0;
        for (Payment payment : stalePayments) {
            String rzpPaymentId = payment.getRazorpayPaymentId();
            if (rzpPaymentId != null && !rzpPaymentId.isBlank()) {
                try {
                    List<com.razorpay.Refund> existingRefunds = razorpayClient.payments.fetchAllRefunds(rzpPaymentId);
                    if (existingRefunds != null && !existingRefunds.isEmpty()) {
                        com.razorpay.Refund existing = existingRefunds.get(0);
                        String refundId = (existing != null && existing.has("id")) ? existing.get("id") : "reconciled";
                        payment.setStatus("REFUNDED");
                        payment.setErrorDescription("Stale refund reconciled: Confirmed existing gateway refund (Refund ID: " + refundId + ")");
                        paymentRepository.save(payment);
                        if (payment.getOrder() != null) {
                            payment.getOrder().setPaymentStatus("REFUNDED");
                            orderRepository.save(payment.getOrder());
                        }
                        log.info("Stale refund #{} reconciled to REFUNDED (Gateway Refund ID: {})", payment.getPaymentId(), refundId);
                        reconciledCount++;
                    } else {
                        payment.setStatus("REFUND_PENDING");
                        payment.setErrorDescription("Stale REFUND_IN_PROGRESS older than " + olderThanMinutes + " min reconciled: No gateway refund found. Flagged for review/retry.");
                        doRecordRefundStatus(payment, "REFUND_PENDING", payment.getErrorDescription());
                        log.warn("ALERT: Stale refund #{} (rzpPaymentId: {}) had no gateway refund. Moved to REFUND_PENDING for review/retry.", payment.getPaymentId(), rzpPaymentId);
                        reconciledCount++;
                    }
                } catch (Exception e) {
                    log.error("ALERT: Failed to query gateway refunds for stale payment #{}: {}. Marked REFUND_PENDING.", payment.getPaymentId(), e.getMessage());
                    payment.setStatus("REFUND_PENDING");
                    payment.setErrorDescription("Stale refund reconciliation error: " + e.getMessage() + ". Flagged as REFUND_PENDING.");
                    doRecordRefundStatus(payment, "REFUND_PENDING", payment.getErrorDescription());
                    reconciledCount++;
                }
            } else {
                payment.setStatus("REFUND_PENDING");
                payment.setErrorDescription("Stale REFUND_IN_PROGRESS without Razorpay payment ID. Flagged for operational review.");
                doRecordRefundStatus(payment, "REFUND_PENDING", payment.getErrorDescription());
                log.warn("ALERT: Stale refund #{} has no Razorpay payment ID. Moved to REFUND_PENDING.", payment.getPaymentId());
                reconciledCount++;
            }
        }
        return reconciledCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentStatusResponse> getPendingRefunds() {
        return paymentRepository.findByStatusOrderByUpdatedAtDesc("REFUND_PENDING").stream()
                .map(p -> mapToStatusResponse(p, null))
                .toList();
    }

    @Override
    @Transactional
    public PaymentStatusResponse retryPendingRefund(Integer paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null for refund retry.");
        }
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (!"REFUND_PENDING".equalsIgnoreCase(payment.getStatus()) && !"REFUND_IN_PROGRESS".equalsIgnoreCase(payment.getStatus())) {
            log.info("Payment #{} is in status '{}', no retry necessary.", paymentId, payment.getStatus());
            return mapToStatusResponse(payment, null);
        }

        String rzpPaymentId = payment.getRazorpayPaymentId();
        if (rzpPaymentId == null || rzpPaymentId.isBlank()) {
            throw new IllegalStateException("Cannot retry gateway refund: Payment #" + paymentId + " has no Razorpay payment ID.");
        }

        try {
            List<com.razorpay.Refund> existingRefunds = razorpayClient.payments.fetchAllRefunds(rzpPaymentId);
            if (existingRefunds != null && !existingRefunds.isEmpty()) {
                com.razorpay.Refund existing = existingRefunds.get(0);
                String refundId = (existing != null && existing.has("id")) ? existing.get("id") : "verified";
                payment.setStatus("REFUNDED");
                payment.setErrorDescription("Retry verified existing gateway refund (Refund ID: " + refundId + ")");
                paymentRepository.save(payment);
                if (payment.getOrder() != null) {
                    payment.getOrder().setPaymentStatus("REFUNDED");
                    orderRepository.save(payment.getOrder());
                }
                log.info("Retry for payment #{} discovered existing gateway refund {}", paymentId, refundId);
                return mapToStatusResponse(payment, null);
            }

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("receipt", "ref_" + rzpPaymentId.trim());
            refundRequest.put("notes", new JSONObject()
                    .put("reason", "Admin retry of pending refund")
                    .put("paymentId", String.valueOf(paymentId)));
            com.razorpay.Refund rzpRefund = razorpayClient.payments.refund(rzpPaymentId, refundRequest);
            String refundId = (rzpRefund != null && rzpRefund.has("id")) ? rzpRefund.get("id") : "processed";
            payment.setStatus("REFUNDED");
            payment.setErrorDescription("Refund successfully executed via retry (Refund ID: " + refundId + ")");
            paymentRepository.save(payment);
            if (payment.getOrder() != null) {
                payment.getOrder().setPaymentStatus("REFUNDED");
                orderRepository.save(payment.getOrder());
            }
            log.info("Retry for payment #{} succeeded with refund ID {}", paymentId, refundId);
        } catch (Exception e) {
            log.error("ALERT: Admin retry of pending refund for payment #{} failed: {}. Payment remains REFUND_PENDING.", paymentId, e.getMessage());
            payment.setStatus("REFUND_PENDING");
            payment.setErrorDescription("Refund retry failed: " + e.getMessage());
            doRecordRefundStatus(payment, "REFUND_PENDING", payment.getErrorDescription());
        }

        return mapToStatusResponse(payment, null);
    }

    @Override
    public int sweepCancelledOrderRefunds(int ageMinutes, int batchSize) {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusMinutes(ageMinutes);
        int effectiveBatchSize = batchSize > 0 ? batchSize : 50;
        List<Payment> candidates = paymentRepository.findCapturedPaymentsForCancelledOrdersOlderThan(
                cutoff, PageRequest.of(0, effectiveBatchSize));

        if (candidates.isEmpty()) {
            return 0;
        }

        log.info("Cancelled-order refund sweep found {} candidate payments older than {} minutes (cutoff: {})",
                candidates.size(), ageMinutes, cutoff);

        int processedCount = 0;
        for (Payment payment : candidates) {
            if (payment.getOrder() == null || payment.getOrder().getOrderId() == null) {
                continue;
            }
            Integer orderId = payment.getOrder().getOrderId();
            try {
                PaymentStatusResponse resp;
                if (self != null) {
                    resp = self.refundOrderPayment(orderId, "Cancelled-order background refund sweep");
                } else {
                    resp = refundOrderPayment(orderId, "Cancelled-order background refund sweep");
                }
                if (resp != null && ("REFUNDED".equalsIgnoreCase(resp.getStatus()) || "REFUNDED_TO_WALLET".equalsIgnoreCase(resp.getStatus()))) {
                    processedCount++;
                }
            } catch (Exception e) {
                log.warn("Cancelled-order refund sweep: Refund failed for Order #{}, Payment #{}: {}",
                        orderId, payment.getPaymentId(), e.getMessage());
                // One failure never stops the rest of the batch
            }
        }

        log.info("Cancelled-order refund sweep completed. Processed {}/{} payments.", processedCount, candidates.size());
        return processedCount;
    }
}
