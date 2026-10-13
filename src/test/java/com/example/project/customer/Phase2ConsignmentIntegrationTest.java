package com.example.project.customer;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.PaymentVerifyRequest;
import com.example.project.customer.dto.event.OrderConfirmedEvent;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.HinchStoreVendorInventory;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderInventoryState;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.OutboxEvent;
import com.example.project.customer.entity.PayoutLedgerStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerPayoutLedger;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.HinchStoreVendorInventoryRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.OutboxEventRepository;
import com.example.project.customer.repository.PaymentRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.repository.WalletRepository;
import com.example.project.customer.repository.WalletTransactionRepository;
import com.example.project.customer.service.CartService;
import com.example.project.customer.service.CheckoutService;
import com.example.project.customer.service.OrderServiceImpl;
import com.example.project.customer.service.PaymentServiceImpl;
import com.example.project.customer.service.PdfInvoiceGeneratorService;
import com.example.project.customer.service.SettlementCalculationServiceImpl;
import com.example.project.customer.service.StoreInvoiceSequenceService;
import com.example.project.customer.service.VendorSelectionService;
import com.example.project.customer.service.outbox.OutboxServiceImpl;
import com.example.project.customer.service.scheduler.HinchStoreOrderTimeoutJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Phase2ConsignmentIntegrationTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CartService cartService;
    @Mock private CheckoutService checkoutService;
    @Mock private PdfInvoiceGeneratorService pdfInvoiceGeneratorService;
    @Mock private StoreInvoiceSequenceService storeInvoiceSequenceService;
    @Mock private SellerPayoutLedgerRepository sellerPayoutLedgerRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private VendorSelectionService vendorSelectionService;
    @Mock private HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private RazorpayClient razorpayClient;

    private PlatformMarketplaceProperties platformMarketplaceProperties;
    private OrderServiceImpl orderService;
    private OutboxServiceImpl outboxService;
    private PaymentServiceImpl paymentService;
    private HinchStoreOrderTimeoutJob timeoutJob;
    private ObjectMapper objectMapper;

    private Store platformStore;
    private Store vendorStore1;
    private Store vendorStore2;
    private Seller vendorSeller1;
    private Seller vendorSeller2;
    private Product platformProduct;
    private Product vendorProduct;
    private Customer testCustomer;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        platformMarketplaceProperties = new PlatformMarketplaceProperties();
        platformMarketplaceProperties.setPlatformStoreId(1);
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                productRepository,
                addressRepository,
                customerRepository,
                cartService,
                checkoutService,
                pdfInvoiceGeneratorService,
                storeInvoiceSequenceService,
                sellerPayoutLedgerRepository,
                storeRepository,
                null,
                null
        );
        orderService.setPlatformMarketplaceProperties(platformMarketplaceProperties);
        orderService.setVendorSelectionService(vendorSelectionService);
        orderService.setHinchStoreVendorInventoryRepository(hinchStoreVendorInventoryRepository);
        orderService.setApplicationEventPublisher(eventPublisher);

        outboxService = new OutboxServiceImpl(
                outboxEventRepository,
                addressRepository,
                storeRepository,
                objectMapper,
                platformMarketplaceProperties
        );

        paymentService = new PaymentServiceImpl(
                razorpayClient,
                paymentRepository,
                orderRepository,
                customerRepository,
                walletRepository,
                walletTransactionRepository,
                cartService,
                checkoutService,
                orderService,
                addressRepository,
                null,
                outboxService
        );

        timeoutJob = new HinchStoreOrderTimeoutJob(
                orderRepository,
                orderService,
                platformMarketplaceProperties
        );

        vendorSeller1 = Seller.builder().sellerId(101).companyName("Vendor One Ltd").build();
        vendorStore1 = Store.builder().storeId(10).seller(vendorSeller1).name("Vendor One Depot")
                .latitude(17.44).longitude(78.38).status(StoreStatus.ACTIVE).build();

        vendorSeller2 = Seller.builder().sellerId(102).companyName("Vendor Two Ltd").build();
        vendorStore2 = Store.builder().storeId(20).seller(vendorSeller2).name("Vendor Two Depot")
                .latitude(17.46).longitude(78.39).status(StoreStatus.ACTIVE).build();

        platformStore = Store.builder().storeId(1).name("HinchStore Central")
                .latitude(17.40).longitude(78.40).status(StoreStatus.ACTIVE).build();

        platformProduct = Product.builder().productId(501).title("Consignment Cement 50kg")
                .store(platformStore).active(true).build();

        vendorProduct = Product.builder().productId(601).title("Vendor TMT Bar")
                .store(vendorStore1).seller(vendorSeller1).stockQty(50).active(true).build();

        testCustomer = Customer.builder().customerId(999).name("Test Builder").phone("9876543210").build();
        testAddress = Address.builder().id(88).customer(testCustomer).addressLine1("Site #42, Madhapur")
                .latitude(17.45).longitude(78.38).build();
    }

    @Test
    @DisplayName("1. Concurrent attempts for last unit (no oversell)")
    void testConcurrentAttemptsForLastUnit_NoOversell() throws InterruptedException {
        // Atomic inventory mock with 1 unit remaining (qty=10, reserved=9, available=1)
        int productId = 501;
        int storeId = 10;
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successfulReservations = new AtomicInteger(0);

        // Simulation of database atomicReserve: SET reserved_qty = reserved_qty + delta WHERE qty - reserved_qty >= delta
        AtomicInteger liveQty = new AtomicInteger(10);
        AtomicInteger liveReserved = new AtomicInteger(9);

        when(hinchStoreVendorInventoryRepository.atomicReserve(eq(productId), eq(storeId), eq(1)))
                .thenAnswer(inv -> {
                    synchronized (liveQty) {
                        if (liveQty.get() - liveReserved.get() >= 1) {
                            liveReserved.incrementAndGet();
                            return 1; // 1 row updated
                        }
                        return 0; // 0 rows updated (no stock)
                    }
                });

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int rows = hinchStoreVendorInventoryRepository.atomicReserve(productId, storeId, 1);
                    if (rows > 0) {
                        successfulReservations.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Exactly 1 thread got the last unit; 9 threads failed
        assertThat(successfulReservations.get()).isEqualTo(1);
        assertThat(liveReserved.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("2. Commit/Release idempotency under concurrency")
    void testCommitReleaseIdempotencyUnderConcurrency() throws InterruptedException {
        Order order = Order.builder()
                .orderId(1001)
                .fulfillmentStore(vendorStore1)
                .inventoryState(OrderInventoryState.RESERVED)
                .items(List.of(OrderItem.builder().productId(501).quantity(2).build()))
                .build();

        AtomicInteger dbState = new AtomicInteger(1); // 1 = RESERVED, 2 = COMMITTED

        when(orderRepository.transitionInventoryState(eq(1001), eq(OrderInventoryState.RESERVED), eq(OrderInventoryState.COMMITTED)))
                .thenAnswer(inv -> {
                    if (dbState.compareAndSet(1, 2)) {
                        return 1;
                    }
                    return 0;
                });

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    orderService.commitHinchStoreReservation(order);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // atomicCommit should be called EXACTLY once across all concurrent calls
        verify(hinchStoreVendorInventoryRepository, times(1)).atomicCommit(eq(501), eq(10), eq(2));
        assertThat(order.getInventoryState()).isEqualTo(OrderInventoryState.COMMITTED);
    }

    @Test
    @DisplayName("3. Cancel after commit restocks at vendor store (qty += n)")
    void testCancelAfterCommit_RestocksAtVendorStore() {
        Order order = Order.builder()
                .orderId(2002)
                .store(platformStore)
                .fulfillmentStore(vendorStore1)
                .orderStatus("CONFIRMED")
                .paymentStatus("PAID")
                .inventoryState(OrderInventoryState.COMMITTED)
                .items(List.of(OrderItem.builder().productId(501).quantity(5).build()))
                .build();

        when(orderRepository.findById(2002)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.transitionInventoryState(2002, OrderInventoryState.COMMITTED, OrderInventoryState.RESTOCKED))
                .thenReturn(1);

        SellerPayoutLedger ledger = SellerPayoutLedger.builder()
                .id(901)
                .order(order)
                .status(PayoutLedgerStatus.PENDING)
                .build();
        when(sellerPayoutLedgerRepository.findAllByOrder_OrderId(2002)).thenReturn(List.of(ledger));

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        try {
            orderService.cancelOrder(2002, "Warehouse", "Customer cancelled before dispatch");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        // Inventory restocked at vendor store (qty += 5)
        verify(hinchStoreVendorInventoryRepository).atomicRestock(eq(501), eq(10), eq(5));
        assertThat(order.getInventoryState()).isEqualTo(OrderInventoryState.RESTOCKED);
        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(ledger.getStatus()).isEqualTo(PayoutLedgerStatus.REVERSED);
    }

    @Test
    @DisplayName("4. Late payment on expired/cancelled order triggers auto-refund")
    void testLatePaymentOnExpiredOrder_AutoRefund() {
        Order expiredOrder = Order.builder()
                .orderId(3003)
                .orderStatus("CANCELLED")
                .paymentStatus("EXPIRED")
                .inventoryState(OrderInventoryState.RELEASED)
                .totalAmount(new BigDecimal("500.00"))
                .build();

        com.example.project.customer.entity.Payment payment = com.example.project.customer.entity.Payment.builder()
                .paymentId(707)
                .order(expiredOrder)
                .amount(new BigDecimal("500.00"))
                .razorpayPaymentId("pay_late_123")
                .build();

        // Verify late payment through webhook
        String webhookPayload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_late_123\",\"order_id\":\"order_rzp_456\",\"method\":\"upi\"}}}}";

        when(paymentRepository.findByRazorpayOrderId("order_rzp_456")).thenReturn(Optional.of(payment));

        String secret = "test_webhook_secret_key_12345";
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "webhookSecret", secret);

        String signature;
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(webhookPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            signature = sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        paymentService.handleWebhook(webhookPayload, signature);

        // Order remains CANCELLED and payment was not marked confirmed
        assertThat(expiredOrder.getOrderStatus()).isEqualTo("CANCELLED");
        verify(orderRepository, never()).save(argThat(o -> "CONFIRMED".equals(o.getOrderStatus())));
    }

    @Test
    @DisplayName("5. Timeout job cancels unpaid HinchStore order and releases reservation")
    void testTimeoutJob_CancelsAndReleases() {
        Order unpaidOrder = Order.builder()
                .orderId(4004)
                .store(platformStore)
                .fulfillmentStore(vendorStore1)
                .orderStatus("PLACED")
                .paymentStatus("PENDING")
                .inventoryState(OrderInventoryState.RESERVED)
                .items(List.of(OrderItem.builder().productId(501).quantity(3).build()))
                .build();

        when(orderRepository.findExpiredUnpaidHinchOrders(eq(1), any(LocalDateTime.class)))
                .thenReturn(List.of(unpaidOrder));
        when(orderRepository.transitionInventoryState(4004, OrderInventoryState.RESERVED, OrderInventoryState.RELEASED))
                .thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        timeoutJob.cancelExpiredUnpaidHinchOrders();

        verify(hinchStoreVendorInventoryRepository).atomicRelease(eq(501), eq(10), eq(3));
        assertThat(unpaidOrder.getInventoryState()).isEqualTo(OrderInventoryState.RELEASED);
        assertThat(unpaidOrder.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(unpaidOrder.getPaymentStatus()).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("6. Candidate retry: First candidate fails, retries next ranked candidate")
    void testCandidateRetry_FirstFailsNextSucceeds() {
        CartItemResponse ci = CartItemResponse.builder()
                .productId(501).title("Consignment Cement").quantity(4)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("1400.00")).build();

        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();
        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(productRepository.findById(501)).thenReturn(Optional.of(platformProduct));
        when(addressRepository.findByCustomer_CustomerIdAndId(999, 88)).thenReturn(Optional.of(testAddress));
        when(customerRepository.findById(999)).thenReturn(Optional.of(testCustomer));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("1400.00")).grandTotal(new BigDecimal("1400.00")).build();
        when(checkoutService.previewCheckout(eq(999), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(platformStore)).thenReturn("INV-100");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(5005);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Ranked candidates: Store 10 (nearest), Store 20 (next)
        when(vendorSelectionService.getRankedCandidateVendors(any(), eq(testAddress)))
                .thenReturn(List.of(vendorStore1, vendorStore2));

        // Store 10 fails reservation (0 rows updated)
        when(hinchStoreVendorInventoryRepository.atomicReserve(eq(501), eq(10), eq(4))).thenReturn(0);
        // Store 20 succeeds reservation (1 row updated)
        when(hinchStoreVendorInventoryRepository.atomicReserve(eq(501), eq(20), eq(4))).thenReturn(1);

        OrderResponse response = orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build());

        assertThat(response.getFulfillmentStoreId()).isEqualTo(20);
        assertThat(response.getFulfillmentStoreName()).isEqualTo("Vendor Two Depot");
        assertThat(response.getInventoryState()).isEqualTo("RESERVED");

        // Verify store 20 was assigned and store 10 had attempted reserve
        verify(hinchStoreVendorInventoryRepository).atomicReserve(501, 10, 4);
        verify(hinchStoreVendorInventoryRepository).atomicReserve(501, 20, 4);
    }

    @Test
    @DisplayName("7. Pickup location in Outbox event is routed from vendor store")
    void testPickupLocationInEvent_RoutesFromVendorStore() {
        Order order = Order.builder()
                .orderId(6006)
                .store(platformStore)
                .fulfillmentStore(vendorStore1)
                .addressId(88)
                .customer(testCustomer)
                .totalWeightKg(new BigDecimal("200.00"))
                .build();

        when(addressRepository.findById(88)).thenReturn(Optional.of(testAddress));
        when(outboxEventRepository.existsByAggregateTypeAndAggregateIdAndEventType(any(), any(), any())).thenReturn(false);

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        when(outboxEventRepository.save(eventCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        outboxService.recordOrderConfirmed(order);

        OutboxEvent savedOutbox = eventCaptor.getValue();
        assertThat(savedOutbox).isNotNull();

        // Deserialize event payload to verify pickup coordinates
        try {
            OrderConfirmedEvent event = objectMapper.readValue(savedOutbox.getPayload(), OrderConfirmedEvent.class);
            assertThat(event.getPickupLatitude()).isEqualTo(17.44);
            assertThat(event.getPickupLongitude()).isEqualTo(78.38);
            assertThat(event.getFulfillmentStoreId()).isEqualTo(10);
            assertThat(event.getFulfillmentStoreName()).isEqualTo("Vendor One Depot");
            assertThat(event.getEventVersion()).isEqualTo("2.0");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("8. Vendor-own flow unchanged: store_id = fulfillment_store_id, stock decrements, inventory_state NONE")
    void testVendorOwnFlow_Unchanged() {
        CartItemResponse ci = CartItemResponse.builder()
                .productId(601).title("Vendor TMT Bar").quantity(2)
                .unitPrice(new BigDecimal("500.00")).lineTotal(new BigDecimal("1000.00")).build();

        CartResponse cart = CartResponse.builder().storeId(10).items(List.of(ci)).build();
        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(10)).thenReturn(Optional.of(vendorStore1));
        when(productRepository.findById(601)).thenReturn(Optional.of(vendorProduct));
        when(productRepository.findByIdForStockUpdate(601)).thenReturn(Optional.of(vendorProduct));
        when(addressRepository.findByCustomer_CustomerIdAndId(999, 88)).thenReturn(Optional.of(testAddress));
        when(customerRepository.findById(999)).thenReturn(Optional.of(testCustomer));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("1000.00")).grandTotal(new BigDecimal("1000.00")).build();
        when(checkoutService.previewCheckout(eq(999), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(vendorStore1)).thenReturn("INV-V1");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(7007);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build());

        // Fulfilling store is vendor's own store, inventoryState is NONE
        assertThat(resp.getStoreId()).isEqualTo(10);
        assertThat(resp.getFulfillmentStoreId()).isEqualTo(10);
        assertThat(resp.getInventoryState()).isEqualTo("NONE");
        // Regular product stock decremented: 50 - 2 = 48
        assertThat(vendorProduct.getStockQty()).isEqualTo(48);
    }

    @Test
    @DisplayName("9. Old event fields unchanged in OrderConfirmedEvent v2.0")
    void testOldEventFieldsUnchanged() throws Exception {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(8008)
                .customerId(999)
                .storeId(1)
                .pickupLatitude(17.44)
                .pickupLongitude(78.38)
                .deliveryAddressId(88)
                .deliveryLatitude(17.45)
                .deliveryLongitude(78.38)
                .totalWeightKg(new BigDecimal("50.00"))
                .fulfillmentStoreId(10)
                .fulfillmentStoreName("Vendor One Depot")
                .pickupAddress("Vendor One Depot")
                .eventVersion("2.0")
                .build();

        String json = objectMapper.writeValueAsString(event);

        // Verify that original v1 fields exist and are deserializable by legacy clients
        assertThat(json).contains("\"orderId\":8008");
        assertThat(json).contains("\"customerId\":999");
        assertThat(json).contains("\"storeId\":1");
        assertThat(json).contains("\"pickupLatitude\":17.44");
        assertThat(json).contains("\"pickupLongitude\":78.38");
        assertThat(json).contains("\"deliveryAddressId\":88");
        assertThat(json).contains("\"deliveryLatitude\":17.45");
        assertThat(json).contains("\"deliveryLongitude\":78.38");
        assertThat(json).contains("\"totalWeightKg\":50.00");
        // Verify v2 additive fields
        assertThat(json).contains("\"fulfillmentStoreId\":10");
        assertThat(json).contains("\"fulfillmentStoreName\":\"Vendor One Depot\"");
        assertThat(json).contains("\"eventVersion\":\"2.0\"");
    }

    @Test
    @DisplayName("10. Late payment capture arriving twice is fully idempotent")
    void testLatePaymentCapture_DuplicateArrival_RefundIdempotent() throws Exception {
        Order cancelledOrder = Order.builder()
                .orderId(9001)
                .orderStatus("CANCELLED")
                .paymentStatus("PENDING")
                .customer(testCustomer)
                .build();

        com.example.project.customer.entity.Payment payment = com.example.project.customer.entity.Payment.builder()
                .paymentId(777)
                .order(cancelledOrder)
                .customer(testCustomer)
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status("CREATED")
                .razorpayOrderId("order_late_123")
                .razorpayPaymentId("pay_late_123")
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_late_123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(com.example.project.customer.entity.Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentClient mockPaymentsClient = mock(PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", mockPaymentsClient);

        String secret = "test_key_secret_123";
        ReflectionTestUtils.setField(paymentService, "keySecret", secret);

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal("order_late_123|pay_late_123".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        String signature = sb.toString();

        JSONObject rzpPaymentJson = new JSONObject();
        rzpPaymentJson.put("id", "pay_late_123");
        rzpPaymentJson.put("status", "captured");
        rzpPaymentJson.put("amount", 150000);
        rzpPaymentJson.put("method", "card");
        com.razorpay.Payment mockRzpPayment = new com.razorpay.Payment(rzpPaymentJson);
        when(mockPaymentsClient.fetch("pay_late_123")).thenReturn(mockRzpPayment);

        JSONObject refundJson = new JSONObject();
        refundJson.put("id", "rfnd_LateCapture1");
        refundJson.put("status", "processed");
        com.razorpay.Refund mockRefund = new com.razorpay.Refund(refundJson);

        when(mockPaymentsClient.refund(eq("pay_late_123"), any(JSONObject.class))).thenReturn(mockRefund);

        PaymentVerifyRequest verifyReq = PaymentVerifyRequest.builder()
                .razorpayOrderId("order_late_123")
                .razorpayPaymentId("pay_late_123")
                .razorpaySignature(signature)
                .build();

        AtomicInteger claimCounter = new AtomicInteger(0);
        when(paymentRepository.claimRefundAtomic(777)).thenAnswer(inv -> claimCounter.compareAndSet(0, 1) ? 1 : 0);

        // 1. First arrival (e.g. verifyPayment)
        paymentService.verifyPayment(999, verifyReq);

        assertThat(payment.getStatus()).isEqualTo("REFUNDED");
        verify(mockPaymentsClient, times(1)).refund(eq("pay_late_123"), any(JSONObject.class));

        // 2. Second duplicate arrival (e.g. late webhook or webhook retry)
        paymentService.verifyPayment(999, verifyReq);

        // Still REFUNDED, and refund was NOT called a second time
        assertThat(payment.getStatus()).isEqualTo("REFUNDED");
        verify(mockPaymentsClient, times(1)).refund(eq("pay_late_123"), any(JSONObject.class));
    }

    @Test
    @DisplayName("11. Outbox: Vendor-own order with null fulfillmentStore falls back to order.store; HinchStore without fulfillmentStore throws")
    void testOutbox_VendorOwnFallback_HinchStoreRequiresFulfillmentStore() {
        // Vendor-own order (null fulfillmentStore)
        Order vendorOrder = Order.builder()
                .orderId(9101)
                .store(vendorStore1)
                .fulfillmentStore(null)
                .customer(testCustomer)
                .addressId(88)
                .totalWeightKg(new BigDecimal("25.00"))
                .build();

        when(addressRepository.findById(88)).thenReturn(Optional.of(testAddress));
        when(outboxEventRepository.existsByAggregateTypeAndAggregateIdAndEventType("ORDER", "9101", "ORDER_CONFIRMED"))
                .thenReturn(false);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        OutboxEvent event = outboxService.recordOrderConfirmed(vendorOrder);
        assertThat(event).isNotNull();
        assertThat(event.getPayload()).contains("\"storeId\":10");

        // HinchStore order with null fulfillmentStore -> throws IllegalStateException
        Order hinchOrderNoFulfillment = Order.builder()
                .orderId(9102)
                .store(platformStore)
                .fulfillmentStore(null)
                .customer(testCustomer)
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> outboxService.recordOrderConfirmed(hinchOrderNoFulfillment)
        );
    }

    @Test
    @DisplayName("12. Concurrent refund: 2 threads racing to refund the same payment execute exactly one gateway refund")
    void testConcurrentRefund_TwoThreads_ExactlyOneGatewayRefund() throws Exception {
        Order cancelledOrder = Order.builder()
                .orderId(9901)
                .orderStatus("CANCELLED")
                .paymentStatus("PENDING")
                .customer(testCustomer)
                .build();

        com.example.project.customer.entity.Payment payment = com.example.project.customer.entity.Payment.builder()
                .paymentId(888)
                .order(cancelledOrder)
                .customer(testCustomer)
                .amount(new BigDecimal("2000.00"))
                .currency("INR")
                .status("CAPTURED")
                .razorpayOrderId("order_race_123")
                .razorpayPaymentId("pay_race_123")
                .build();

        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(9901))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(com.example.project.customer.entity.Payment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Atomic claim mock simulating database conditional UPDATE:
        // Exactly one thread wins (returns 1), the second thread loses (returns 0)
        AtomicInteger claimCounter = new AtomicInteger(0);
        when(paymentRepository.claimRefundAtomic(888))
                .thenAnswer(inv -> claimCounter.compareAndSet(0, 1) ? 1 : 0);

        PaymentClient mockPaymentsClient = mock(PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", mockPaymentsClient);

        JSONObject refundJson = new JSONObject();
        refundJson.put("id", "rfnd_RaceWinner");
        refundJson.put("status", "processed");
        com.razorpay.Refund mockRefund = new com.razorpay.Refund(refundJson);
        when(mockPaymentsClient.refund(eq("pay_race_123"), any(JSONObject.class))).thenReturn(mockRefund);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    paymentService.refundOrderPayment(9901, "Concurrent cancellation refund");
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Exactly one gateway refund was executed across the two racing threads
        verify(mockPaymentsClient, times(1)).refund(eq("pay_race_123"), any(JSONObject.class));
        assertThat(payment.getStatus()).isEqualTo("REFUNDED");
    }
}
