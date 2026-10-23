package com.example.project.customer;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.*;
import com.example.project.customer.dto.settlement.ItemSettlementSnapshot;
import com.example.project.customer.entity.*;
import com.example.project.customer.event.OrderCancelledEvent;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.repository.*;
import com.example.project.customer.service.*;
import com.example.project.customer.service.helper.PaymentRefundExecutionHelper;
import com.example.project.customer.service.outbox.OutboxService;
import com.razorpay.OrderClient;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MultiStoreCartAndCheckoutRegressionTest {

    // Repositories
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderGroupRepository orderGroupRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private SellerPayoutLedgerRepository sellerPayoutLedgerRepository;

    // Services
    @Mock private CouponService couponService;
    @Mock private CheckoutService checkoutService;
    @Mock private PdfInvoiceGeneratorService pdfInvoiceGeneratorService;
    @Mock private StoreInvoiceSequenceService storeInvoiceSequenceService;
    @Mock private TaxCalculationService taxCalculationService;
    @Mock private OutboxService outboxService;
    @Mock private UserContextUtil userContextUtil;
    @Mock private PaymentRefundExecutionHelper paymentRefundExecutionHelper;
    @Mock private RazorpayClient razorpayClient;

    private CartServiceImpl cartService;
    private OrderServiceImpl orderService;
    private PaymentServiceImpl paymentService;

    // Test Data
    private Customer testCustomer;
    private Store storeA;
    private Store storeB;
    private Seller sellerA;
    private Seller sellerB;
    private Product productA;
    private Product productB;
    private Address address;
    private Cart cart;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // 1. Setup Cart Service
        cartService = new CartServiceImpl(cartRepository, cartItemRepository, productRepository, storeRepository, couponService);

        // 2. Setup Order Service
        orderService = new OrderServiceImpl(
                orderRepository, orderItemRepository, productRepository, addressRepository, customerRepository,
                cartService, checkoutService, couponService, pdfInvoiceGeneratorService, storeInvoiceSequenceService,
                sellerPayoutLedgerRepository, storeRepository, userContextUtil, null
        );
        orderService.setOrderGroupRepository(orderGroupRepository);
        PlatformMarketplaceProperties props = new PlatformMarketplaceProperties();
        props.setPlatformStoreId(1);
        props.setHinchStoreCheckoutEnabled(true);
        orderService.setPlatformMarketplaceProperties(props);

        // 3. Setup Payment Service
        paymentService = new PaymentServiceImpl(
                razorpayClient, paymentRepository, orderRepository, customerRepository,
                walletRepository, walletTransactionRepository, cartService, checkoutService,
                orderService, addressRepository, userContextUtil, outboxService
        );
        ReflectionTestUtils.setField(paymentService, "orderGroupRepository", orderGroupRepository);
        ReflectionTestUtils.setField(paymentService, "sellerPayoutLedgerRepository", sellerPayoutLedgerRepository);
        ReflectionTestUtils.setField(paymentService, "paymentRefundExecutionHelper", paymentRefundExecutionHelper);
        ReflectionTestUtils.setField(paymentService, "keyId", "rzp_test_key");
        ReflectionTestUtils.setField(paymentService, "keySecret", "rzp_test_secret");
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "test_webhook_secret");

        // Mock common refund atomic claim
        when(paymentRepository.claimRefundAtomic(any())).thenReturn(1);
        when(paymentRefundExecutionHelper.claimRefundAtomic(any())).thenReturn(1);

        // Test Entities
        testCustomer = Customer.builder().customerId(100).name("Test Customer").email("cust@test.com").phone("9000000000").build();

        sellerA = Seller.builder().sellerId(1).name("Seller A").state("Telangana").build();
        storeA = Store.builder().storeId(10).seller(sellerA).name("Store Alpha").slug("store-alpha").status(StoreStatus.ACTIVE).build();

        sellerB = Seller.builder().sellerId(2).name("Seller B").state("Karnataka").build();
        storeB = Store.builder().storeId(20).seller(sellerB).name("Store Beta").slug("store-beta").status(StoreStatus.ACTIVE).build();

        productA = Product.builder().productId(101).store(storeA).seller(sellerA).title("Cement Bags").price(BigDecimal.valueOf(350.00)).stockQty(100).active(true).approvalStatus(ApprovalStatus.APPROVED).build();
        productB = Product.builder().productId(202).store(storeB).seller(sellerB).title("Steel Rods").price(BigDecimal.valueOf(1500.00)).stockQty(50).active(true).approvalStatus(ApprovalStatus.APPROVED).build();

        address = Address.builder().id(50).customer(testCustomer).addressLine1("Site 1").city("Hyderabad").state("Telangana").pincode("500081").build();

        cart = Cart.builder().cartId(1).customer(testCustomer).items(new ArrayList<>()).isActive(true).build();

        when(customerRepository.findById(100)).thenReturn(Optional.of(testCustomer));
        when(addressRepository.findByCustomer_CustomerIdAndId(100, 50)).thenReturn(Optional.of(address));
        when(productRepository.findById(101)).thenReturn(Optional.of(productA));
        when(productRepository.findById(202)).thenReturn(Optional.of(productB));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(storeRepository.findById(10)).thenReturn(Optional.of(storeA));
        when(storeRepository.findById(20)).thenReturn(Optional.of(storeB));
        when(cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(100)).thenReturn(Optional.of(cart));
        when(cartRepository.findByCustomer_CustomerId(100)).thenReturn(List.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        when(userContextUtil.getCurrentUserId()).thenReturn(100);
        when(userContextUtil.getOptionalCurrentUserId()).thenReturn(100);

        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> {
            OrderItem oi = i.getArgument(0);
            if (oi.getOrderItemId() == null) oi.setOrderItemId(new Random().nextInt(1000) + 1000);
            return oi;
        });

        when(cartItemRepository.findByCart_CartId(1)).thenAnswer(i -> new ArrayList<>(cart.getItems()));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> {
            CartItem ci = i.getArgument(0);
            if (ci.getCartItemId() == null) ci.setCartItemId(new Random().nextInt(1000) + 1);
            if (!cart.getItems().contains(ci)) {
                cart.getItems().add(ci);
            }
            return ci;
        });
        doAnswer(i -> {
            CartItem ci = i.getArgument(0);
            cart.getItems().remove(ci);
            return null;
        }).when(cartItemRepository).delete(any(CartItem.class));
        doAnswer(i -> {
            cart.getItems().clear();
            return null;
        }).when(cartItemRepository).deleteByCart_CartId(any());
        when(cartItemRepository.findAllByCart_CartIdAndProduct_ProductId(any(), any())).thenAnswer(i -> {
            Integer pId = i.getArgument(1);
            return cart.getItems().stream()
                    .filter(item -> item.getProduct() != null && item.getProduct().getProductId().equals(pId))
                    .collect(java.util.stream.Collectors.toList());
        });
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(any(), any())).thenAnswer(i -> {
            Integer pId = i.getArgument(1);
            return cart.getItems().stream()
                    .filter(item -> item.getProduct() != null && item.getProduct().getProductId().equals(pId))
                    .findFirst();
        });

        when(orderGroupRepository.save(any(OrderGroup.class))).thenAnswer(i -> {
            OrderGroup og = i.getArgument(0);
            if (og.getId() == null) og.setId(777);
            return og;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(new Random().nextInt(1000) + 1000);
            return o;
        });
    }

    @Test
    @DisplayName("1. Multi-store cart: allows products from multiple stores, groups items by store, maintains grand total")
    void testMultiStoreCart_AddProductsFromDifferentStores_GroupsByStoreAndMaintainsGrandTotal() {
        CartItem itemA = CartItem.builder().cartItemId(1).cart(cart).product(productA).quantity(2).build();
        CartItem itemB = CartItem.builder().cartItemId(2).cart(cart).product(productB).quantity(1).build();
        cart.getItems().add(itemA);
        cart.getItems().add(itemB);

        CartResponse resp = cartService.calculateCartResponse(cart);

        assertThat(resp).isNotNull();
        assertThat(resp.getItems()).hasSize(2);
        assertThat(resp.getSubtotal()).isEqualByComparingTo("2200.00");
        assertThat(resp.getTotalGst()).isEqualByComparingTo("396.00");
        assertThat(resp.getGrandTotal()).isEqualByComparingTo("2596.00");

        // Verify storeGroups
        assertThat(resp.getStoreGroups()).hasSize(2);
        CartStoreGroupResponse grpA = resp.getStoreGroups().stream().filter(g -> g.getStoreId().equals(10)).findFirst().orElseThrow();
        assertThat(grpA.getStoreName()).isEqualTo("Store Alpha");
        assertThat(grpA.getSubtotal()).isEqualByComparingTo("700.00");
        assertThat(grpA.getGrandTotal()).isEqualByComparingTo("826.00");
        assertThat(grpA.getItems()).hasSize(1);

        CartStoreGroupResponse grpB = resp.getStoreGroups().stream().filter(g -> g.getStoreId().equals(20)).findFirst().orElseThrow();
        assertThat(grpB.getStoreName()).isEqualTo("Store Beta");
        assertThat(grpB.getSubtotal()).isEqualByComparingTo("1500.00");
        assertThat(grpB.getGrandTotal()).isEqualByComparingTo("1770.00");
        assertThat(grpB.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("2. Cart synchronization: merges guest cart items across multiple stores without silent dropping")
    void testCartSync_MergesMultiStoreItemsWithoutDropping() {
        // Customer already has Store A item in DB
        CartItem existingItem = CartItem.builder().cartItemId(1).cart(cart).product(productA).quantity(1).build();
        cart.getItems().add(existingItem);

        // Guest sync request brings another Store A item + Store B item
        CartSyncRequest syncReq = CartSyncRequest.builder()
                .items(List.of(
                        CartItemRequest.builder().productId(101).quantity(3).build(),
                        CartItemRequest.builder().productId(202).quantity(2).build()
                ))
                .build();

        CartResponse syncedResp = cartService.syncCart(100, syncReq);

        assertThat(syncedResp).isNotNull();
        assertThat(syncedResp.getItems()).hasSize(2);
        // Product 101 updated: 3
        CartItemResponse respA = syncedResp.getItems().stream().filter(i -> i.getProductId().equals(101)).findFirst().orElseThrow();
        assertThat(respA.getQuantity()).isEqualTo(3);

        // Product 202 added: 2
        CartItemResponse respB = syncedResp.getItems().stream().filter(i -> i.getProductId().equals(202)).findFirst().orElseThrow();
        assertThat(respB.getQuantity()).isEqualTo(2);

        // Grouped by store
        assertThat(syncedResp.getStoreGroups()).hasSize(2);
    }

    @Test
    @DisplayName("3. Multi-store order creation: creates 1 OrderGroup and 2 child Order records with store invoices and ledgers")
    void testCreateOrder_CreatesOrderGroupWithSeparateChildOrdersAndLedgers() {
        CartItem itemA = CartItem.builder().cartItemId(1).cart(cart).product(productA).quantity(2).build();
        CartItem itemB = CartItem.builder().cartItemId(2).cart(cart).product(productB).quantity(1).build();
        cart.getItems().add(itemA);
        cart.getItems().add(itemB);
        when(cartItemRepository.findByCart_CartId(1)).thenReturn(cart.getItems());

        // Mock Checkout Preview
        CartStoreGroupResponse previewGrpA = CartStoreGroupResponse.builder()
                .storeId(10).storeName("Store Alpha")
                .subtotal(BigDecimal.valueOf(700.00)).discount(BigDecimal.ZERO)
                .taxableAmount(BigDecimal.valueOf(700.00))
                .cgst(BigDecimal.valueOf(63.00)).sgst(BigDecimal.valueOf(63.00)).igst(BigDecimal.ZERO)
                .totalGst(BigDecimal.valueOf(126.00)).deliveryCharge(BigDecimal.valueOf(50.00))
                .grandTotal(BigDecimal.valueOf(876.00)).canCheckout(true)
                .build();
        CartStoreGroupResponse previewGrpB = CartStoreGroupResponse.builder()
                .storeId(20).storeName("Store Beta")
                .subtotal(BigDecimal.valueOf(1500.00)).discount(BigDecimal.ZERO)
                .taxableAmount(BigDecimal.valueOf(1500.00))
                .cgst(BigDecimal.ZERO).sgst(BigDecimal.ZERO).igst(BigDecimal.valueOf(270.00))
                .totalGst(BigDecimal.valueOf(270.00)).deliveryCharge(BigDecimal.valueOf(100.00))
                .grandTotal(BigDecimal.valueOf(1870.00)).canCheckout(true)
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(BigDecimal.valueOf(2200.00))
                .discount(BigDecimal.ZERO)
                .taxableAmount(BigDecimal.valueOf(2200.00))
                .cgst(BigDecimal.valueOf(63.00)).sgst(BigDecimal.valueOf(63.00)).igst(BigDecimal.valueOf(270.00))
                .totalGst(BigDecimal.valueOf(396.00)).freightCharge(BigDecimal.valueOf(150.00))
                .craneUnloadingCharge(BigDecimal.ZERO)
                .grandTotal(BigDecimal.valueOf(2746.00))
                .storeGroups(List.of(previewGrpA, previewGrpB))
                .canPlaceOrder(true)
                .build();
        when(checkoutService.previewCheckout(eq(100), any(CheckoutPreviewRequest.class))).thenReturn(preview);

        // Store invoice sequence mock
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(eq(storeA))).thenReturn("INV-ALPHA-001");
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(eq(storeB))).thenReturn("INV-BETA-001");

        // Locking queries
        when(productRepository.findByIdForStockUpdate(101)).thenReturn(Optional.of(productA));
        when(productRepository.findByIdForStockUpdate(202)).thenReturn(Optional.of(productB));

        OrderCreateRequest orderReq = OrderCreateRequest.builder().addressId(50).paymentMethod("RAZORPAY").build();
        OrderResponse resp = orderService.createOrder(100, orderReq);

        assertThat(resp).isNotNull();
        assertThat(resp.getOrderGroupId()).isEqualTo(777);
        assertThat(resp.getChildOrders()).hasSize(2);

        // Verify OrderGroup was saved with aggregate amounts
        ArgumentCaptor<OrderGroup> ogCaptor = ArgumentCaptor.forClass(OrderGroup.class);
        verify(orderGroupRepository, atLeastOnce()).save(ogCaptor.capture());
        OrderGroup savedGroup = ogCaptor.getValue();
        assertThat(savedGroup.getGrandTotal()).isEqualByComparingTo("2746.00");
        assertThat(savedGroup.getOrders()).hasSize(2);

        // Verify child orders
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeast(2)).save(orderCaptor.capture());
        List<Order> capturedOrders = orderCaptor.getAllValues();
        assertThat(capturedOrders.stream().anyMatch(o -> o.getStore().getStoreId().equals(10) && "INV-ALPHA-001".equals(o.getStoreInvoiceNumber()))).isTrue();
        assertThat(capturedOrders.stream().anyMatch(o -> o.getStore().getStoreId().equals(20) && "INV-BETA-001".equals(o.getStoreInvoiceNumber()))).isTrue();

        // Verify separate seller payout ledgers created
        verify(sellerPayoutLedgerRepository, atLeast(2)).save(any(SellerPayoutLedger.class));

        // Verify cart is cleared
        verify(cartItemRepository).deleteByCart_CartId(1);
    }

    @Test
    @DisplayName("4. Deadlock prevention: items locked in ascending productId order")
    void testCreateOrder_EnforcesAscendingProductIdDeadlockPrevention() {
        // Items in reverse productId order: 202 before 101
        CartItem itemB = CartItem.builder().cartItemId(1).cart(cart).product(productB).quantity(1).build();
        CartItem itemA = CartItem.builder().cartItemId(2).cart(cart).product(productA).quantity(1).build();
        cart.getItems().add(itemB);
        cart.getItems().add(itemA);
        when(cartItemRepository.findByCart_CartId(1)).thenReturn(cart.getItems());

        when(productRepository.findByIdForStockUpdate(101)).thenReturn(Optional.of(productA));
        when(productRepository.findByIdForStockUpdate(202)).thenReturn(Optional.of(productB));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .grandTotal(BigDecimal.valueOf(1850.00))
                .canPlaceOrder(true)
                .build();
        when(checkoutService.previewCheckout(eq(100), any(CheckoutPreviewRequest.class))).thenReturn(preview);

        OrderCreateRequest orderReq = OrderCreateRequest.builder().addressId(50).build();
        orderService.createOrder(100, orderReq);

        // Verify product 101 is locked BEFORE product 202
        org.mockito.InOrder inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).findByIdForStockUpdate(101);
        inOrder.verify(productRepository).findByIdForStockUpdate(202);
    }

    @Test
    @DisplayName("5. Atomic rollback: if Store B has insufficient stock, Store A reservation is rolled back")
    void testCreateOrder_AtomicRollbackOnStoreBFailure() {
        productA.setStockQty(10);
        productB.setStockQty(0); // Store B item out of stock

        CartItem itemA = CartItem.builder().cartItemId(1).cart(cart).product(productA).quantity(2).build();
        CartItem itemB = CartItem.builder().cartItemId(2).cart(cart).product(productB).quantity(5).build();
        cart.getItems().add(itemA);
        cart.getItems().add(itemB);
        when(cartItemRepository.findByCart_CartId(1)).thenReturn(cart.getItems());

        when(productRepository.findByIdForStockUpdate(101)).thenReturn(Optional.of(productA));
        when(productRepository.findByIdForStockUpdate(202)).thenReturn(Optional.of(productB));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .grandTotal(BigDecimal.valueOf(8200.00))
                .canPlaceOrder(true)
                .build();
        when(checkoutService.previewCheckout(eq(100), any(CheckoutPreviewRequest.class))).thenReturn(preview);

        OrderCreateRequest orderReq = OrderCreateRequest.builder().addressId(50).build();

        assertThatThrownBy(() -> orderService.createOrder(100, orderReq))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Insufficient stock");

        // Verify productA stock was restored back to 10
        assertThat(productA.getStockQty()).isEqualTo(10);
        // Verify no order group was persisted
        verify(orderGroupRepository, never()).save(any(OrderGroup.class));
    }

    @Test
    @DisplayName("6. Single Razorpay payment creation for OrderGroup")
    void testCreatePaymentOrder_ForOrderGroup() throws Exception {
        OrderGroup group = OrderGroup.builder()
                .id(777)
                .groupOrderNumber("HM-GRP-777")
                .customer(testCustomer)
                .grandTotal(BigDecimal.valueOf(2746.00))
                .groupStatus("PLACED")
                .build();
        when(orderGroupRepository.findById(777)).thenReturn(Optional.of(group));

        OrderClient mockOrdersClient = mock(OrderClient.class);
        ReflectionTestUtils.setField(razorpayClient, "orders", mockOrdersClient);
        com.razorpay.Order mockRzpOrder = mock(com.razorpay.Order.class);
        when(mockRzpOrder.get("id")).thenReturn("order_RzpGroup123");
        when(mockOrdersClient.create(any(JSONObject.class))).thenReturn(mockRzpOrder);

        PaymentOrderCreateRequest req = PaymentOrderCreateRequest.builder()
                .orderGroupId(777)
                .build();

        PaymentOrderCreateResponse resp = paymentService.createPaymentOrder(100, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getOrderGroupId()).isEqualTo(777);
        assertThat(resp.getGroupOrderNumber()).isEqualTo("HM-GRP-777");
        assertThat(resp.getAmount()).isEqualByComparingTo("2746.00");
        assertThat(resp.getAmountInPaise()).isEqualTo(274600L);

        ArgumentCaptor<Payment> pCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(pCaptor.capture());
        Payment savedPayment = pCaptor.getValue();
        assertThat(savedPayment.getOrderGroup().getId()).isEqualTo(777);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo("2746.00");
    }

    @Test
    @DisplayName("7. Payment verification: confirms OrderGroup and all child orders")
    void testVerifyPayment_ConfirmsOrderGroupAndChildOrders() throws Exception {
        OrderGroup group = OrderGroup.builder()
                .id(777)
                .groupOrderNumber("HM-GRP-777")
                .groupStatus("PLACED")
                .paymentStatus("PENDING")
                .build();
        Order child1 = Order.builder().orderId(1001).orderGroup(group).orderStatus("PLACED").paymentStatus("PENDING").checkpoints(new ArrayList<>()).build();
        Order child2 = Order.builder().orderId(1002).orderGroup(group).orderStatus("PLACED").paymentStatus("PENDING").checkpoints(new ArrayList<>()).build();

        Payment payment = Payment.builder()
                .paymentId(99)
                .orderGroup(group)
                .razorpayOrderId("order_RzpGroup123")
                .amount(BigDecimal.valueOf(2746.00))
                .status("CREATED")
                .purpose("ORDER_PAYMENT")
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_RzpGroup123")).thenReturn(Optional.of(payment));
        when(orderRepository.findByOrderGroup_Id(777)).thenReturn(List.of(child1, child2));

        // Mock Razorpay payment fetch
        PaymentClient mockPayments = mock(PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", mockPayments);
        com.razorpay.Payment rzpPayment = mock(com.razorpay.Payment.class);
        when(rzpPayment.get("status")).thenReturn("captured");
        when(rzpPayment.get("amount")).thenReturn(274600L);
        when(mockPayments.fetch("pay_Rzp123")).thenReturn(rzpPayment);

        PaymentVerifyRequest verifyReq = PaymentVerifyRequest.builder()
                .razorpayOrderId("order_RzpGroup123")
                .razorpayPaymentId("pay_Rzp123")
                .razorpaySignature("sig")
                .build();

        // Bypass cryptographic HMAC check in test unit
        try (var mockedUtils = mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyPaymentSignature(any(JSONObject.class), any()))
                    .thenReturn(true);

            PaymentStatusResponse resp = paymentService.verifyPayment(100, verifyReq);

            assertThat(resp).isNotNull();
            assertThat(resp.getStatus()).isEqualTo("CAPTURED");

            // Verify OrderGroup updated
            assertThat(group.getGroupStatus()).isEqualTo("CONFIRMED");
            assertThat(group.getPaymentStatus()).isEqualTo("PAID");

            // Verify both child orders marked CONFIRMED and PAID
            assertThat(child1.getOrderStatus()).isEqualTo("CONFIRMED");
            assertThat(child1.getPaymentStatus()).isEqualTo("PAID");
            assertThat(child2.getOrderStatus()).isEqualTo("CONFIRMED");
            assertThat(child2.getPaymentStatus()).isEqualTo("PAID");

            verify(outboxService).recordOrderConfirmed(child1);
            verify(outboxService).recordOrderConfirmed(child2);
        }
    }

    @Test
    @DisplayName("8. Partial refund: cancelling Store A child order triggers partial refund without breaking sibling Store B")
    void testCancelChildOrder_TriggersPartialRefundAndUpdatesOrderGroup() throws Exception {
        OrderGroup group = OrderGroup.builder()
                .id(777)
                .customer(testCustomer)
                .groupStatus("CONFIRMED")
                .paymentStatus("PAID")
                .build();
        Order child1 = Order.builder()
                .orderId(1001)
                .customer(testCustomer)
                .store(storeA)
                .orderGroup(group)
                .totalAmount(BigDecimal.valueOf(876.00))
                .orderStatus("PLACED")
                .paymentStatus("PAID")
                .checkpoints(new ArrayList<>())
                .items(List.of(OrderItem.builder().productId(101).title("Cement Bags").quantity(2).unitPrice(BigDecimal.valueOf(350.00)).unit("BAG").build()))
                .build();
        Order child2 = Order.builder()
                .orderId(1002)
                .customer(testCustomer)
                .store(storeB)
                .orderGroup(group)
                .totalAmount(BigDecimal.valueOf(1870.00))
                .orderStatus("PLACED")
                .paymentStatus("PAID")
                .checkpoints(new ArrayList<>())
                .items(List.of(OrderItem.builder().productId(202).title("Steel Rods").quantity(1).unitPrice(BigDecimal.valueOf(1500.00)).unit("TON").build()))
                .build();

        Payment payment = Payment.builder()
                .paymentId(99)
                .orderGroup(group)
                .customer(testCustomer)
                .razorpayPaymentId("pay_Rzp123")
                .amount(BigDecimal.valueOf(2746.00))
                .status("CAPTURED")
                .build();

        when(orderRepository.findById(1001)).thenReturn(Optional.of(child1));
        when(orderRepository.findById(1002)).thenReturn(Optional.of(child2));
        when(orderRepository.findByOrderGroup_Id(777)).thenReturn(List.of(child1, child2));
        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(1001)).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderGroup_IdOrderByCreatedAtDesc(777)).thenReturn(Optional.of(payment));

        PaymentClient mockPayments = mock(PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", mockPayments);
        com.razorpay.Refund mockRefund = mock(com.razorpay.Refund.class);
        when(mockRefund.has("id")).thenReturn(true);
        when(mockRefund.get("id")).thenReturn("rfnd_child1_partial");
        when(mockPayments.refund(eq("pay_Rzp123"), any(JSONObject.class))).thenReturn(mockRefund);

        // Cancel child1
        OrderResponse cancelResp = orderService.cancelOrder(1001, "Customer Request", "Cancel Store A item");
        assertThat(cancelResp).isNotNull();
        assertThat(child1.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(group.getGroupStatus()).isEqualTo("PARTIALLY_CANCELLED");

        // Trigger payment refund for child1
        PaymentStatusResponse refundResp = paymentService.refundOrderPayment(1001, "Cancel Store A item");

        assertThat(refundResp).isNotNull();
        assertThat(child1.getPaymentStatus()).isEqualTo("REFUNDED");
        assertThat(payment.getStatus()).isEqualTo("PARTIALLY_REFUNDED");
        assertThat(group.getPaymentStatus()).isEqualTo("PARTIALLY_REFUNDED");

        // Verify Razorpay refund called with child1 amount (876.00 INR = 87600 paise)
        ArgumentCaptor<JSONObject> rzpReqCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(mockPayments).refund(eq("pay_Rzp123"), rzpReqCaptor.capture());
        JSONObject rzpJson = rzpReqCaptor.getValue();
        assertThat(rzpJson.getLong("amount")).isEqualTo(87600L);

        // Sibling child2 remains active and PAID
        assertThat(child2.getOrderStatus()).isEqualTo("PLACED");
        assertThat(child2.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("9. Full cancellation: cancelling all child orders transitions OrderGroup and Payment to REFUNDED")
    void testCancelAllChildOrders_FullRefund() throws Exception {
        OrderGroup group = OrderGroup.builder()
                .id(777)
                .customer(testCustomer)
                .groupStatus("PARTIALLY_CANCELLED")
                .paymentStatus("PARTIALLY_REFUNDED")
                .build();
        Order child1 = Order.builder().orderId(1001).customer(testCustomer).store(storeA).orderGroup(group).totalAmount(BigDecimal.valueOf(876.00)).orderStatus("CANCELLED").paymentStatus("REFUNDED").checkpoints(new ArrayList<>()).build();
        Order child2 = Order.builder().orderId(1002).customer(testCustomer).store(storeB).orderGroup(group).totalAmount(BigDecimal.valueOf(1870.00)).orderStatus("PLACED").paymentStatus("PAID").checkpoints(new ArrayList<>()).items(List.of(OrderItem.builder().productId(202).title("Steel Rods").quantity(1).unitPrice(BigDecimal.valueOf(1500.00)).unit("TON").build())).build();

        Payment payment = Payment.builder()
                .paymentId(99)
                .orderGroup(group)
                .customer(testCustomer)
                .razorpayPaymentId("pay_Rzp123")
                .amount(BigDecimal.valueOf(2746.00))
                .status("PARTIALLY_REFUNDED")
                .build();

        when(orderRepository.findById(1002)).thenReturn(Optional.of(child2));
        when(orderRepository.findByOrderGroup_Id(777)).thenReturn(List.of(child1, child2));
        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(1002)).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderGroup_IdOrderByCreatedAtDesc(777)).thenReturn(Optional.of(payment));

        PaymentClient mockPayments = mock(PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", mockPayments);
        com.razorpay.Refund mockRefund = mock(com.razorpay.Refund.class);
        when(mockRefund.has("id")).thenReturn(true);
        when(mockRefund.get("id")).thenReturn("rfnd_child2_final");
        when(mockPayments.refund(eq("pay_Rzp123"), any(JSONObject.class))).thenReturn(mockRefund);

        // Cancel child2
        orderService.cancelOrder(1002, "Customer Request", "Cancel Store B item");
        assertThat(group.getGroupStatus()).isEqualTo("CANCELLED");

        // Trigger payment refund for child2
        paymentService.refundOrderPayment(1002, "Cancel Store B item");

        assertThat(child2.getPaymentStatus()).isEqualTo("REFUNDED");
        assertThat(payment.getStatus()).isEqualTo("REFUNDED");
        assertThat(group.getPaymentStatus()).isEqualTo("REFUNDED");
    }

    @Test
    @DisplayName("10. Legacy single-store order compatibility: order_group_id null works seamlessly")
    void testLegacySingleStoreOrder_OrderGroupIdNullCompatible() {
        Order legacyOrder = Order.builder()
                .orderId(999)
                .orderNumber("ORD-LEGACY-001")
                .orderGroup(null) // legacy: no group
                .customer(testCustomer)
                .store(storeA)
                .totalAmount(BigDecimal.valueOf(500.00))
                .orderStatus("PLACED")
                .paymentStatus("PENDING")
                .build();

        Payment legacyPayment = Payment.builder()
                .paymentId(55)
                .order(legacyOrder)
                .orderGroup(null)
                .customer(testCustomer)
                .amount(BigDecimal.valueOf(500.00))
                .status("CAPTURED")
                .build();

        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(999)).thenReturn(Optional.of(legacyPayment));

        PaymentStatusResponse statusResp = paymentService.getLatestOrderPaymentStatus(999);
        assertThat(statusResp).isNotNull();
        assertThat(statusResp.getOrderId()).isEqualTo(999);
        assertThat(statusResp.getStatus()).isEqualTo("CAPTURED");
        assertThat(statusResp.getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("11. Background sweep: processes unrefunded cancelled child orders under OrderGroup")
    void testSweepCancelledOrderRefunds_ProcessesChildOrdersUnderOrderGroup() throws Exception {
        OrderGroup group = OrderGroup.builder()
                .id(777)
                .groupOrderNumber("HM-GRP-777")
                .groupStatus("PARTIALLY_CANCELLED")
                .paymentStatus("PARTIALLY_REFUNDED")
                .build();
        Order child1 = Order.builder().orderId(1001).store(storeA).orderGroup(group).totalAmount(BigDecimal.valueOf(876.00)).orderStatus("CANCELLED").paymentStatus("REFUNDED").checkpoints(new ArrayList<>()).build();
        Order child2 = Order.builder().orderId(1002).store(storeB).orderGroup(group).totalAmount(BigDecimal.valueOf(1870.00)).orderStatus("CANCELLED").paymentStatus("PAID").checkpoints(new ArrayList<>()).build();

        Payment payment = Payment.builder()
                .paymentId(99)
                .orderGroup(group)
                .customer(testCustomer)
                .razorpayPaymentId("pay_Rzp123")
                .amount(BigDecimal.valueOf(2746.00))
                .status("PARTIALLY_REFUNDED")
                .build();

        when(paymentRepository.findCapturedPaymentsForCancelledOrdersOlderThan(any(LocalDateTime.class), anyInt(), any()))
                .thenReturn(List.of(payment))
                .thenReturn(Collections.emptyList());
        when(orderRepository.findByOrderGroup_Id(777)).thenReturn(List.of(child1, child2));
        when(orderRepository.findById(1002)).thenReturn(Optional.of(child2));
        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(1002)).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderGroup_IdOrderByCreatedAtDesc(777)).thenReturn(Optional.of(payment));

        PaymentClient mockPayments = mock(PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", mockPayments);
        com.razorpay.Refund mockRefund = mock(com.razorpay.Refund.class);
        when(mockRefund.has("id")).thenReturn(true);
        when(mockRefund.get("id")).thenReturn("rfnd_child2_sweep");
        when(mockPayments.refund(eq("pay_Rzp123"), any(JSONObject.class))).thenReturn(mockRefund);

        int swept = paymentService.sweepCancelledOrderRefunds(10, 50);

        assertThat(swept).isEqualTo(1);
        assertThat(child2.getPaymentStatus()).isEqualTo("REFUNDED");
        assertThat(payment.getStatus()).isEqualTo("REFUNDED");
        assertThat(group.getPaymentStatus()).isEqualTo("REFUNDED");
    }

    @Test
    @DisplayName("12. Stale refund reconciliation: cascades confirmed refund to OrderGroup and child orders")
    void testReconcileStaleRefunds_CascadesToOrderGroup() throws Exception {
        OrderGroup group = OrderGroup.builder()
                .id(777)
                .groupStatus("CANCELLED")
                .paymentStatus("PARTIALLY_REFUNDED")
                .build();
        Order child = Order.builder().orderId(1001).orderGroup(group).orderStatus("CANCELLED").paymentStatus("PAID").build();
        Payment payment = Payment.builder()
                .paymentId(99)
                .orderGroup(group)
                .razorpayPaymentId("pay_Rzp123")
                .status("REFUND_IN_PROGRESS")
                .build();

        when(paymentRepository.findByStatusAndUpdatedAtBefore(eq("REFUND_IN_PROGRESS"), any(LocalDateTime.class)))
                .thenReturn(List.of(payment));
        when(orderRepository.findByOrderGroup_Id(777)).thenReturn(List.of(child));

        PaymentClient mockPayments = mock(PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", mockPayments);
        com.razorpay.Refund rzpRefund = mock(com.razorpay.Refund.class);
        when(rzpRefund.has("id")).thenReturn(true);
        when(rzpRefund.get("id")).thenReturn("rfnd_reconciled_123");
        when(mockPayments.fetchAllRefunds("pay_Rzp123")).thenReturn(List.of(rzpRefund));

        int reconciled = paymentService.reconcileStaleRefunds(15);

        assertThat(reconciled).isEqualTo(1);
        assertThat(payment.getStatus()).isEqualTo("REFUNDED");
        assertThat(group.getPaymentStatus()).isEqualTo("REFUNDED");
        assertThat(child.getPaymentStatus()).isEqualTo("REFUNDED");
    }

    @Test
    @DisplayName("13. HinchStore timeout job: cascades expired cancellation to OrderGroup")
    void testHinchStoreTimeoutJob_CascadesToOrderGroup() {
        OrderGroup group = OrderGroup.builder()
                .id(777)
                .groupStatus("PLACED")
                .paymentStatus("PENDING")
                .build();
        Order child = Order.builder()
                .orderId(1001)
                .orderGroup(group)
                .orderStatus("PLACED")
                .paymentStatus("PENDING")
                .checkpoints(new ArrayList<>())
                .build();

        when(orderRepository.findExpiredUnpaidHinchOrders(eq(1), any(LocalDateTime.class)))
                .thenReturn(List.of(child));
        when(orderRepository.findByOrderGroup_Id(777)).thenReturn(List.of(child));

        com.example.project.customer.service.scheduler.HinchStoreOrderTimeoutJob timeoutJob =
                new com.example.project.customer.service.scheduler.HinchStoreOrderTimeoutJob(
                        orderRepository, orderService, null, orderGroupRepository);

        timeoutJob.cancelExpiredUnpaidHinchOrders();

        assertThat(child.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(child.getPaymentStatus()).isEqualTo("EXPIRED");
        assertThat(group.getGroupStatus()).isEqualTo("CANCELLED");
        assertThat(group.getPaymentStatus()).isEqualTo("EXPIRED");
        verify(orderGroupRepository).save(group);
    }

    @Test
    @DisplayName("14. Idempotency: duplicate captured webhook does not duplicate outbox events or double-commit")
    void testDuplicateWebhook_Idempotent_NoDuplicateOutboxEvents() throws Exception {
        OrderGroup group = OrderGroup.builder()
                .id(777)
                .groupOrderNumber("HM-GRP-777")
                .groupStatus("CONFIRMED")
                .paymentStatus("PAID")
                .build();
        Order child1 = Order.builder().orderId(1001).orderGroup(group).orderStatus("CONFIRMED").paymentStatus("PAID").checkpoints(new ArrayList<>()).build();
        Order child2 = Order.builder().orderId(1002).orderGroup(group).orderStatus("CONFIRMED").paymentStatus("PAID").checkpoints(new ArrayList<>()).build();

        Payment payment = Payment.builder()
                .paymentId(99)
                .orderGroup(group)
                .razorpayOrderId("order_RzpGroup123")
                .status("CAPTURED")
                .amount(BigDecimal.valueOf(2746.00))
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_RzpGroup123")).thenReturn(Optional.of(payment));
        when(orderRepository.findByOrderGroup_Id(777)).thenReturn(List.of(child1, child2));

        String webhookPayload = "{" +
                "\"event\":\"payment.captured\"," +
                "\"payload\":{" +
                "  \"payment\":{" +
                "    \"entity\":{" +
                "      \"id\":\"pay_Rzp123\"," +
                "      \"order_id\":\"order_RzpGroup123\"," +
                "      \"amount\":274600," +
                "      \"method\":\"upi\"" +
                "    }" +
                "  }" +
                "}}";

        try (var mockedUtils = mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyWebhookSignature(anyString(), anyString(), anyString()))
                    .thenReturn(true);

            paymentService.handleWebhook(webhookPayload, "valid_mock_signature");

            // Since orders were already CONFIRMED, no new order confirmed event should be fired
            verify(outboxService, never()).recordOrderConfirmed(any());
        }
    }

    @Test
    @DisplayName("15. Late capture: payment captured after OrderGroup cancellation triggers automatic refund")
    void testLatePaymentCapture_AfterGroupCancellation_TriggersAutoRefund() throws Exception {
        OrderGroup group = OrderGroup.builder()
                .id(777)
                .groupOrderNumber("HM-GRP-777")
                .groupStatus("CANCELLED")
                .paymentStatus("EXPIRED")
                .build();

        Payment payment = Payment.builder()
                .paymentId(99)
                .orderGroup(group)
                .customer(testCustomer)
                .razorpayOrderId("order_RzpGroup123")
                .status("CREATED")
                .amount(BigDecimal.valueOf(2746.00))
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_RzpGroup123")).thenReturn(Optional.of(payment));
        when(orderRepository.findByOrderGroup_Id(777)).thenReturn(Collections.emptyList());

        PaymentClient mockPayments = mock(PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", mockPayments);
        com.razorpay.Refund mockRefund = mock(com.razorpay.Refund.class);
        when(mockRefund.has("id")).thenReturn(true);
        when(mockRefund.get("id")).thenReturn("rfnd_late_capture");
        when(mockPayments.refund(eq("pay_Rzp123"), any(JSONObject.class))).thenReturn(mockRefund);

        String webhookPayload = "{" +
                "\"event\":\"payment.captured\"," +
                "\"payload\":{" +
                "  \"payment\":{" +
                "    \"entity\":{" +
                "      \"id\":\"pay_Rzp123\"," +
                "      \"order_id\":\"order_RzpGroup123\"," +
                "      \"amount\":274600," +
                "      \"method\":\"card\"" +
                "    }" +
                "  }" +
                "}}";

        try (var mockedUtils = mockStatic(com.razorpay.Utils.class)) {
            mockedUtils.when(() -> com.razorpay.Utils.verifyWebhookSignature(anyString(), anyString(), anyString()))
                    .thenReturn(true);

            paymentService.handleWebhook(webhookPayload, "valid_mock_signature");

            // Verify status is REFUNDED and refund was sent to gateway
            assertThat(payment.getStatus()).isEqualTo("REFUNDED");
            verify(mockPayments).refund(eq("pay_Rzp123"), any(JSONObject.class));
        }
    }

    @Test
    @DisplayName("16. Idempotency: repeated refundOrderPayment does not call gateway twice")
    void testRepeatedRefund_Idempotent_NoDuplicateGatewayCalls() throws Exception {
        OrderGroup group = OrderGroup.builder().id(777).paymentStatus("REFUNDED").build();
        Order child = Order.builder().orderId(1001).orderGroup(group).paymentStatus("REFUNDED").orderStatus("CANCELLED").build();
        Payment payment = Payment.builder().paymentId(99).orderGroup(group).status("REFUNDED").amount(BigDecimal.valueOf(500.00)).build();

        when(orderRepository.findById(1001)).thenReturn(Optional.of(child));
        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(1001)).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderGroup_IdOrderByCreatedAtDesc(777)).thenReturn(Optional.of(payment));

        PaymentClient mockPayments = mock(PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", mockPayments);

        PaymentStatusResponse resp = paymentService.refundOrderPayment(1001, "Repeated cancel call");

        assertThat(resp).isNotNull();
        // Since already REFUNDED, mockPayments.refund must NOT be called
        verify(mockPayments, never()).refund(anyString(), any(JSONObject.class));
    }
}
