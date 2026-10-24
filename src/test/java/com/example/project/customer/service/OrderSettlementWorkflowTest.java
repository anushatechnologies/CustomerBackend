package com.example.project.customer.service;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.CommissionRule;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.PayoutLedgerStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerPayoutLedger;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CommissionRuleRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.repository.HinchStoreVendorInventoryRepository;
import com.example.project.customer.service.VendorSelectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderSettlementWorkflowTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CartService cartService;
    @Mock private CheckoutService checkoutService;
    @Mock private CouponService couponService;
    @Mock private PdfInvoiceGeneratorService pdfInvoiceGeneratorService;
    @Mock private StoreInvoiceSequenceService storeInvoiceSequenceService;
    @Mock private SellerPayoutLedgerRepository sellerPayoutLedgerRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private UserContextUtil userContextUtil;
    @Mock private SellerContextUtil sellerContextUtil;
    @Mock private CommissionRuleRepository commissionRuleRepository;
    @Mock private VendorSelectionService vendorSelectionService;
    @Mock private HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;

    private SettlementCalculationServiceImpl settlementCalculationService;
    private PlatformMarketplaceProperties platformMarketplaceProperties;
    private OrderServiceImpl orderService;

    private Seller vendorSeller;
    private Store vendorStore;
    private Store hinchStore;
    private Store vendorStoreWithoutSeller;
    private Address deliveryAddress;
    private Customer mockCustomer;
    private Product vendorProduct1;
    private Product vendorProduct2;
    private Product hinchProduct;
    private Subcategory mockSubcategory;

    @BeforeEach
    void setUp() {
        settlementCalculationService = new SettlementCalculationServiceImpl(commissionRuleRepository);
        platformMarketplaceProperties = new PlatformMarketplaceProperties();
        platformMarketplaceProperties.setPlatformStoreId(1);
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                productRepository,
                addressRepository,
                customerRepository,
                cartService,
                checkoutService,
                couponService,
                pdfInvoiceGeneratorService,
                storeInvoiceSequenceService,
                sellerPayoutLedgerRepository,
                storeRepository,
                userContextUtil,
                sellerContextUtil
        );
        orderService.setSettlementCalculationService(settlementCalculationService);
        orderService.setPlatformMarketplaceProperties(platformMarketplaceProperties);
        orderService.setVendorSelectionService(vendorSelectionService);
        orderService.setHinchStoreVendorInventoryRepository(hinchStoreVendorInventoryRepository);

        vendorSeller = Seller.builder().sellerId(42).name("Vendor Seller Co").build();
        vendorStore = Store.builder().storeId(10).seller(vendorSeller).name("Vendor Steel Depot").status(StoreStatus.ACTIVE).build();
        vendorStoreWithoutSeller = Store.builder().storeId(20).seller(null).name("Vendor Depot Without Seller").status(StoreStatus.ACTIVE).build();

        hinchStore = Store.builder().storeId(1).name("HinchStore Central").status(StoreStatus.ACTIVE).build();

        deliveryAddress = Address.builder().id(99).addressLine1("123 Site Road").city("Hyderabad").state("Telangana").build();
        mockCustomer = Customer.builder().customerId(5).build();

        vendorProduct1 = Product.builder()
                .productId(201)
                .title("Vendor Cement 50kg")
                .seller(vendorSeller)
                .store(vendorStore)
                .stockQty(100)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .price(new BigDecimal("400.00"))
                .build();

        vendorProduct2 = Product.builder()
                .productId(202)
                .title("Vendor Sand 1 Ton")
                .seller(vendorSeller)
                .store(vendorStore)
                .stockQty(50)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .price(new BigDecimal("600.00"))
                .build();

        mockSubcategory = Subcategory.builder()
                .subcategoryId(20)
                .category(Category.builder().categoryId(7).build())
                .build();

        hinchProduct = Product.builder()
                .productId(301)
                .title("HinchStore Aggregate")
                .seller(null)
                .store(hinchStore)
                .stockQty(200)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .price(new BigDecimal("1000.00"))
                .subcategory(mockSubcategory)
                .build();
    }

    @Test
    @DisplayName("1. Vendor-own product order: snapshot items, payee from seller, gross excludes GST & freight")
    void testVendorOwnProductOrder_SnapshotsSettlementAndExcludesGstFreight() {
        int userId = 5;

        CartItemResponse ci = CartItemResponse.builder()
                .productId(201)
                .title("Vendor Cement 50kg")
                .quantity(2)
                .unit("BAG")
                .unitPrice(new BigDecimal("400.00"))
                .lineTotal(new BigDecimal("800.00"))
                .lineGst(new BigDecimal("144.00"))
                .build();

        CartResponse cart = CartResponse.builder()
                .storeId(10)
                .items(List.of(ci))
                .subtotal(new BigDecimal("800.00"))
                .grandTotal(new BigDecimal("1094.00")) // 800 + 144 GST + 100 Freight + 50 Crane
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("800.00"))
                .discount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("800.00"))
                .totalGst(new BigDecimal("144.00"))
                .freightCharge(new BigDecimal("100.00"))
                .craneUnloadingCharge(new BigDecimal("50.00"))
                .grandTotal(new BigDecimal("1094.00"))
                .build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(10)).thenReturn(Optional.of(vendorStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(vendorProduct1));
        when(productRepository.findByIdForStockUpdate(201)).thenReturn(Optional.of(vendorProduct1));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(vendorStore)).thenReturn("INV-VND-001");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(1001);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).build();
        orderService.createOrder(userId, req);

        // Verify OrderItem snapshot
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getSeller().getSellerId()).isEqualTo(42);
        assertThat(savedItem.getFulfillmentStore().getStoreId()).isEqualTo(10);
        assertThat(savedItem.getIsHinchStore()).isFalse();
        assertThat(savedItem.getCommissionRate()).isEqualByComparingTo("0.00");
        assertThat(savedItem.getSellerPayable()).isEqualByComparingTo("800.00");
        assertThat(savedItem.getHinchKeeps()).isEqualByComparingTo("0.00");

        // Verify SellerPayoutLedger snapshot
        ArgumentCaptor<SellerPayoutLedger> ledgerCaptor = ArgumentCaptor.forClass(SellerPayoutLedger.class);
        verify(sellerPayoutLedgerRepository).save(ledgerCaptor.capture());
        SellerPayoutLedger savedLedger = ledgerCaptor.getValue();

        assertThat(savedLedger.getSeller().getSellerId()).isEqualTo(42);
        assertThat(savedLedger.getStore().getStoreId()).isEqualTo(10);
        assertThat(savedLedger.getStatus()).isEqualTo(PayoutLedgerStatus.PENDING);
        // Gross must be strictly seller_payable (800.00), excluding GST (144.00) and shipping (150.00)
        assertThat(savedLedger.getGrossAmount()).isEqualByComparingTo("800.00");
        // 1% TCS on taxable seller payable: 800 * 0.01 = 8.00
        assertThat(savedLedger.getTcsAmount()).isEqualByComparingTo("8.00");
        assertThat(savedLedger.getNetPayoutAmount()).isEqualByComparingTo("792.00");
    }

    @Test
    @DisplayName("2. HinchStore product order with feature flag OFF: rejected with IllegalStateException, no order/ledger/stock change")
    void testHinchStoreOrder_FlagDisabled_RejectedImmediately() {
        int userId = 5;
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        CartItemResponse ci = CartItemResponse.builder().productId(301).quantity(1).build();
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(hinchStore));

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).build();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> orderService.createOrder(userId, req));

        assertThat(ex.getMessage()).contains("HinchStore checkout is temporarily disabled");

        // Verify zero side effects: no order saved, no stock decremented, no ledger created
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verify(sellerPayoutLedgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. HinchStore order with feature flag ON: item seller is null, ledger is ON_HOLD, fee uses PRE-discount")
    void testHinchStoreOrder_FlagEnabled_ItemSellerNull_LedgerOnHold() {
        int userId = 5;
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        CartItemResponse ci = CartItemResponse.builder()
                .productId(301)
                .title("HinchStore Aggregate")
                .quantity(1)
                .unit("TON")
                .unitPrice(new BigDecimal("1000.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .lineGst(new BigDecimal("180.00"))
                .build();

        CartResponse cart = CartResponse.builder()
                .storeId(1)
                .items(List.of(ci))
                .subtotal(new BigDecimal("1000.00"))
                .grandTotal(new BigDecimal("1080.00")) // 1000 + 180 GST - 100 Discount
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("1000.00"))
                .discount(new BigDecimal("100.00")) // 100 coupon discount
                .taxableAmount(new BigDecimal("900.00"))
                .totalGst(new BigDecimal("180.00"))
                .grandTotal(new BigDecimal("1080.00"))
                .build();

        CommissionRule rule = CommissionRule.builder()
                .ruleId(1)
                .rate(new BigDecimal("7.50")) // 7.5% holding fee
                .isActive(true)
                .build();

        when(commissionRuleRepository.findActiveGlobalRules(any())).thenReturn(List.of(rule));
        when(commissionRuleRepository.findApplicableRules(eq(7), any())).thenReturn(List.of(rule));

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(hinchStore));
        when(productRepository.findById(301)).thenReturn(Optional.of(hinchProduct));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        Store vendorStoreWithoutSeller = Store.builder().storeId(10).seller(null).name("Vendor Steel Depot").status(StoreStatus.ACTIVE).build();
        when(vendorSelectionService.getRankedCandidateVendors(any(), eq(deliveryAddress))).thenReturn(List.of(vendorStoreWithoutSeller));
        when(hinchStoreVendorInventoryRepository.atomicReserve(eq(301), eq(vendorStore.getStoreId()), eq(1))).thenReturn(1);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(2001);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).build();
        orderService.createOrder(userId, req);

        // Verify OrderItem: seller is NULL, isHinchStore is true
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getSeller()).isNull();
        assertThat(savedItem.getIsHinchStore()).isTrue();
        assertThat(savedItem.getCommissionRate()).isEqualByComparingTo("7.50");
        // Holding fee = 7.5% of PRE-discount (1000 * 0.075 = 75.00)
        assertThat(savedItem.getSellerPayable()).isEqualByComparingTo("75.00");
        // Customer paid 900.00. Hinch keeps 900 - 75 = 825.00
        assertThat(savedItem.getHinchKeeps()).isEqualByComparingTo("825.00");

        // Verify SellerPayoutLedger: status is ON_HOLD, seller is NULL
        ArgumentCaptor<SellerPayoutLedger> ledgerCaptor = ArgumentCaptor.forClass(SellerPayoutLedger.class);
        verify(sellerPayoutLedgerRepository).save(ledgerCaptor.capture());
        SellerPayoutLedger savedLedger = ledgerCaptor.getValue();

        assertThat(savedLedger.getSeller()).isNull();
        assertThat(savedLedger.getStatus()).isEqualTo(PayoutLedgerStatus.ON_HOLD);
        assertThat(savedLedger.getGrossAmount()).isEqualByComparingTo("75.00");
        assertThat(savedLedger.getTcsAmount()).isEqualByComparingTo("0.00");
        assertThat(savedLedger.getNetPayoutAmount()).isEqualByComparingTo("75.00");
        assertThat(savedLedger.getHinchKeeps()).isEqualByComparingTo("825.00");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private void authenticateAsAdmin() {
        com.example.project.customer.security.FirebaseUserPrincipal principal =
                com.example.project.customer.security.FirebaseUserPrincipal.builder()
                        .firebaseUid("admin-uid")
                        .internalUserId(999)
                        .email("admin@hinchmart.com")
                        .role(com.example.project.customer.entity.Role.ADMIN)
                        .authorities(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                        .build();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()
                )
        );
    }

    @Test
    @DisplayName("4. DELIVERED: PENDING ledger stays PENDING with T+7 settlement date, idempotent on 2nd call, ON_HOLD is untouched")
    void testDelivered_StatusPromotion_Idempotent_OnHoldUntouched() {
        authenticateAsAdmin();
        int orderId = 5001;
        Order mockOrder = Order.builder().orderId(orderId).orderStatus("DISPATCHED").checkpoints(new ArrayList<>()).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime originalSettlementDate = LocalDateTime.now().plusDays(7);

        // Ledger 1: PENDING -> Stays PENDING with settlementDate = now + 7 days
        SellerPayoutLedger pendingLedger = SellerPayoutLedger.builder()
                .id(101)
                .order(mockOrder)
                .status(PayoutLedgerStatus.PENDING)
                .build();

        // Ledger 2: ON_HOLD -> Must NOT be modified
        SellerPayoutLedger onHoldLedger = SellerPayoutLedger.builder()
                .id(102)
                .order(mockOrder)
                .status(PayoutLedgerStatus.ON_HOLD)
                .build();

        when(sellerPayoutLedgerRepository.findAllByOrder_OrderId(orderId))
                .thenReturn(List.of(pendingLedger, onHoldLedger));

        // First DELIVERED update
        orderService.updateOrderStatus(orderId, "DELIVERED", "Site", "Goods delivered");

        assertThat(pendingLedger.getStatus()).isEqualTo(PayoutLedgerStatus.PENDING);
        assertThat(pendingLedger.getSettlementDate()).isNotNull();
        LocalDateTime firstAssignedDate = pendingLedger.getSettlementDate();

        // ON_HOLD ledger must remain ON_HOLD with no settlement date
        assertThat(onHoldLedger.getStatus()).isEqualTo(PayoutLedgerStatus.ON_HOLD);
        assertThat(onHoldLedger.getSettlementDate()).isNull();

        // Second DELIVERED call: Idempotency check
        orderService.updateOrderStatus(orderId, "DELIVERED", "Site", "Duplicate callback");

        assertThat(pendingLedger.getStatus()).isEqualTo(PayoutLedgerStatus.PENDING);
        // Settlement date must not change on duplicate delivery callback
        assertThat(pendingLedger.getSettlementDate()).isEqualTo(firstAssignedDate);
        assertThat(onHoldLedger.getStatus()).isEqualTo(PayoutLedgerStatus.ON_HOLD);
    }

    @Test
    @DisplayName("5. Multi-item order with coupon: sum(seller_payable) + sum(hinch_keeps) reconciles with customer paid, no lost paise")
    void testMultiItemOrderWithCoupon_ExactPennyReconciliation() {
        int userId = 5;

        // Item 1: 400.00 pre-discount
        CartItemResponse ci1 = CartItemResponse.builder()
                .productId(201)
                .title("Vendor Cement 50kg")
                .quantity(1)
                .unitPrice(new BigDecimal("400.00"))
                .lineTotal(new BigDecimal("400.00"))
                .lineGst(new BigDecimal("72.00"))
                .build();

        // Item 2: 600.00 pre-discount
        CartItemResponse ci2 = CartItemResponse.builder()
                .productId(202)
                .title("Vendor Sand 1 Ton")
                .quantity(1)
                .unitPrice(new BigDecimal("600.00"))
                .lineTotal(new BigDecimal("600.00"))
                .lineGst(new BigDecimal("108.00"))
                .build();

        // Total subtotal: 1000.00, coupon discount: 150.00 (Customer pays 850.00 merchandise)
        CartResponse cart = CartResponse.builder()
                .storeId(10)
                .items(List.of(ci1, ci2))
                .subtotal(new BigDecimal("1000.00"))
                .grandTotal(new BigDecimal("1030.00"))
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("1000.00"))
                .discount(new BigDecimal("150.00"))
                .taxableAmount(new BigDecimal("850.00"))
                .totalGst(new BigDecimal("180.00"))
                .grandTotal(new BigDecimal("1030.00"))
                .build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(10)).thenReturn(Optional.of(vendorStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(vendorProduct1));
        when(productRepository.findById(202)).thenReturn(Optional.of(vendorProduct2));
        when(productRepository.findByIdForStockUpdate(201)).thenReturn(Optional.of(vendorProduct1));
        when(productRepository.findByIdForStockUpdate(202)).thenReturn(Optional.of(vendorProduct2));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(vendorStore)).thenReturn("INV-VND-002");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(3001);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).build();
        orderService.createOrder(userId, req);

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, times(2)).save(itemCaptor.capture());
        List<OrderItem> savedItems = itemCaptor.getAllValues();

        BigDecimal sumSellerPayable = savedItems.stream()
                .map(OrderItem::getSellerPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sumHinchKeeps = savedItems.stream()
                .map(OrderItem::getHinchKeeps)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Platform funds coupons: Vendor gets full 1000.00 (400 + 600)
        assertThat(sumSellerPayable).isEqualByComparingTo("1000.00");
        // Hinch keeps negative 150.00 discount absorption
        assertThat(sumHinchKeeps).isEqualByComparingTo("-150.00");

        // Perfect reconciliation with customer-paid merchandise (850.00), zero lost paise
        BigDecimal netReconciled = sumSellerPayable.add(sumHinchKeeps);
        BigDecimal customerPaidTaxable = preview.getTaxableAmount();
        assertThat(netReconciled).isEqualByComparingTo(customerPaidTaxable);
    }

    private void authenticateAsSeller(int sellerId) {
        com.example.project.customer.security.FirebaseUserPrincipal principal =
                com.example.project.customer.security.FirebaseUserPrincipal.builder()
                        .firebaseUid("seller-uid-" + sellerId)
                        .internalUserId(sellerId)
                        .sellerId(sellerId)
                        .email("seller@hinchmart.com")
                        .role(com.example.project.customer.entity.Role.SELLER)
                        .authorities(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SELLER")))
                        .build();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()
                )
        );
    }

    @Test
    @DisplayName("7. Restrict DELIVERED status update: Sellers cannot mark DELIVERED; Only Admin can")
    void testDeliveredPermission_SellerForbidden_AdminAllowed() {
        int orderId = 6001;
        Order mockOrder = Order.builder()
                .orderId(orderId)
                .orderStatus("DISPATCHED")
                .store(vendorStore)
                .checkpoints(new ArrayList<>())
                .build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // 1. Authenticated seller attempts DELIVERED -> throws ForbiddenException
        authenticateAsSeller(vendorSeller.getSellerId());
        org.junit.jupiter.api.Assertions.assertThrows(
                com.example.project.customer.exception.ForbiddenException.class,
                () -> orderService.updateOrderStatus(orderId, "DELIVERED", "Site", "Delivered")
        );

        // 2. Admin attempts DELIVERED -> succeeds
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sellerPayoutLedgerRepository.findAllByOrder_OrderId(orderId)).thenReturn(List.of());
        authenticateAsAdmin();
        OrderResponse adminResp = orderService.updateOrderStatus(orderId, "DELIVERED", "Site", "Delivered");
        assertThat(adminResp.getOrderStatus()).isEqualTo("DELIVERED");
    }

    @Test
    @DisplayName("8. When customer orders seller's product even if store resolves to HinchStore (storeId: 1), OrderItem seller_id is preserved and 0% commission applied")
    void testCheckout_SellerProductUnderAdminStore_SellerIdPreservedOnOrderItem() {
        int userId = 5;
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        CartItemResponse item = CartItemResponse.builder()
                .cartItemId(1)
                .productId(201)
                .title("Vendor Cement 50kg")
                .quantity(2)
                .unit("Bag")
                .unitPrice(new BigDecimal("400.00"))
                .lineTotal(new BigDecimal("800.00"))
                .lineGst(new BigDecimal("144.00"))
                .build();
        CartResponse cart = CartResponse.builder()
                .cartId(1)
                .storeId(1)
                .items(List.of(item))
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("800.00"))
                .discount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("800.00"))
                .totalGst(new BigDecimal("144.00"))
                .freightCharge(BigDecimal.ZERO)
                .craneUnloadingCharge(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("944.00"))
                .build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(hinchStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(vendorProduct1));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(hinchStore)).thenReturn("INV-HINCH-002");
        when(vendorSelectionService.getRankedCandidateVendors(any(), eq(deliveryAddress))).thenReturn(List.of(vendorStore));
        when(hinchStoreVendorInventoryRepository.atomicReserve(eq(201), eq(vendorStore.getStoreId()), eq(2))).thenReturn(1);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(1002);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).build();
        orderService.createOrder(userId, req);

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getSeller()).isNotNull();
        assertThat(savedItem.getSeller().getSellerId()).isEqualTo(42);
        assertThat(savedItem.getIsHinchStore()).isFalse();
        assertThat(savedItem.getCommissionRate()).isEqualByComparingTo("0.00");
        assertThat(savedItem.getSellerPayable()).isEqualByComparingTo("800.00");
        assertThat(savedItem.getHinchKeeps()).isEqualByComparingTo("0.00");

        // Verify commissionRuleRepository was never queried for vendor product
        org.mockito.Mockito.verifyNoInteractions(commissionRuleRepository);
    }

    @Test
    @DisplayName("9. Regression: Vendor-owned COD order succeeds with 0% commission even when no active commission rules exist")
    void testVendorOwnedCodOrder_SucceedsWithZeroCommissionWithoutRules() {
        int userId = 5;

        CartItemResponse ci = CartItemResponse.builder()
                .productId(201)
                .title("Vendor Cement 50kg")
                .quantity(2)
                .unit("BAG")
                .unitPrice(new BigDecimal("400.00"))
                .lineTotal(new BigDecimal("800.00"))
                .lineGst(new BigDecimal("144.00"))
                .build();

        CartResponse cart = CartResponse.builder()
                .storeId(10)
                .items(List.of(ci))
                .subtotal(new BigDecimal("800.00"))
                .grandTotal(new BigDecimal("944.00"))
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("800.00"))
                .discount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("800.00"))
                .totalGst(new BigDecimal("144.00"))
                .grandTotal(new BigDecimal("944.00"))
                .build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(10)).thenReturn(Optional.of(vendorStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(vendorProduct1));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(vendorStore)).thenReturn("INV-VND-COD");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(3001);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).paymentMethod("COD").build();
        OrderResponse resp = orderService.createOrder(userId, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getOrderId()).isEqualTo(3001);

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getCommissionRate()).isEqualByComparingTo("0.00");
        assertThat(savedItem.getSellerPayable()).isEqualByComparingTo("800.00");
        assertThat(savedItem.getHinchKeeps()).isEqualByComparingTo("0.00");
        assertThat(savedItem.getIsHinchStore()).isFalse();

        org.mockito.Mockito.verifyNoInteractions(commissionRuleRepository);
    }

    @Test
    @DisplayName("10. Regression: Vendor-owned online order uses 0% commission")
    void testVendorOwnedOnlineOrder_UsesZeroCommission() {
        int userId = 5;

        CartItemResponse ci = CartItemResponse.builder()
                .productId(201)
                .title("Vendor Cement 50kg")
                .quantity(1)
                .unit("BAG")
                .unitPrice(new BigDecimal("400.00"))
                .lineTotal(new BigDecimal("400.00"))
                .lineGst(new BigDecimal("72.00"))
                .build();

        CartResponse cart = CartResponse.builder()
                .storeId(10)
                .items(List.of(ci))
                .subtotal(new BigDecimal("400.00"))
                .grandTotal(new BigDecimal("472.00"))
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("400.00"))
                .discount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("400.00"))
                .totalGst(new BigDecimal("72.00"))
                .grandTotal(new BigDecimal("472.00"))
                .build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(10)).thenReturn(Optional.of(vendorStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(vendorProduct1));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(vendorStore)).thenReturn("INV-VND-ONLINE");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(3002);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).paymentMethod("ONLINE").build();
        OrderResponse resp = orderService.createOrder(userId, req);

        assertThat(resp).isNotNull();

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getCommissionRate()).isEqualByComparingTo("0.00");
        assertThat(savedItem.getSellerPayable()).isEqualByComparingTo("400.00");
        assertThat(savedItem.getHinchKeeps()).isEqualByComparingTo("0.00");
        assertThat(savedItem.getIsHinchStore()).isFalse();

        org.mockito.Mockito.verifyNoInteractions(commissionRuleRepository);
    }

    @Test
    @DisplayName("11. Regression: HinchStore order uses the configured global holding fee")
    void testHinchStoreOrder_UsesConfiguredGlobalHoldingFee() {
        int userId = 5;
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        CartItemResponse ci = CartItemResponse.builder()
                .productId(301)
                .title("HinchStore Aggregate")
                .quantity(1)
                .unitPrice(new BigDecimal("1000.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .build();
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();
        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("1000.00")).grandTotal(new BigDecimal("1000.00")).build();

        CommissionRule globalRule = CommissionRule.builder()
                .ruleId(1)
                .category(null)
                .rate(new BigDecimal("8.50")) // 8.5% holding fee
                .isActive(true)
                .build();

        when(commissionRuleRepository.findActiveGlobalRules(any())).thenReturn(List.of(globalRule));
        when(commissionRuleRepository.findApplicableRules(any(), any())).thenReturn(List.of(globalRule));

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(hinchStore));
        when(productRepository.findById(301)).thenReturn(Optional.of(hinchProduct));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(vendorSelectionService.getRankedCandidateVendors(any(), eq(deliveryAddress))).thenReturn(List.of(vendorStore));
        when(hinchStoreVendorInventoryRepository.atomicReserve(eq(301), eq(vendorStore.getStoreId()), eq(1))).thenReturn(1);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(4001);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(userId, OrderCreateRequest.builder().addressId(99).build());

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getCommissionRate()).isEqualByComparingTo("8.50");
        assertThat(savedItem.getSellerPayable()).isEqualByComparingTo("85.00"); // 1000 * 0.085
        assertThat(savedItem.getHinchKeeps()).isEqualByComparingTo("915.00");
    }

    @Test
    @DisplayName("12. Regression: Category-specific rule overrides the global rule")
    void testHinchStoreOrder_CategorySpecificRuleOverridesGlobalRule() {
        int userId = 5;
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        CartItemResponse ci = CartItemResponse.builder()
                .productId(301)
                .title("HinchStore Aggregate")
                .quantity(1)
                .unitPrice(new BigDecimal("1000.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .build();
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();
        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("1000.00")).grandTotal(new BigDecimal("1000.00")).build();

        CommissionRule globalRule = CommissionRule.builder()
                .ruleId(1).category(null).rate(new BigDecimal("5.00")).isActive(true).build();
        CommissionRule categoryRule = CommissionRule.builder()
                .ruleId(2).category(Category.builder().categoryId(7).build()).rate(new BigDecimal("12.00")).isActive(true).build();

        when(commissionRuleRepository.findActiveGlobalRules(any())).thenReturn(List.of(globalRule));
        when(commissionRuleRepository.findApplicableRules(eq(7), any())).thenReturn(List.of(categoryRule, globalRule));

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(hinchStore));
        when(productRepository.findById(301)).thenReturn(Optional.of(hinchProduct));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(vendorSelectionService.getRankedCandidateVendors(any(), eq(deliveryAddress))).thenReturn(List.of(vendorStore));
        when(hinchStoreVendorInventoryRepository.atomicReserve(eq(301), eq(vendorStore.getStoreId()), eq(1))).thenReturn(1);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(4002);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(userId, OrderCreateRequest.builder().addressId(99).build());

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        OrderItem savedItem = itemCaptor.getValue();

        // 12.00% category rule applied instead of 5.00% global
        assertThat(savedItem.getCommissionRate()).isEqualByComparingTo("12.00");
        assertThat(savedItem.getSellerPayable()).isEqualByComparingTo("120.00");
        assertThat(savedItem.getHinchKeeps()).isEqualByComparingTo("880.00");
    }

    @Test
    @DisplayName("13. Regression: Missing applicable HinchStore rule fails with a clear actionable error")
    void testHinchStoreOrder_MissingApplicableRule_FailsWithActionableError() {
        int userId = 5;
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        CartItemResponse ci = CartItemResponse.builder()
                .productId(301)
                .title("HinchStore Aggregate")
                .quantity(1)
                .unitPrice(new BigDecimal("1000.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .build();
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();
        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("1000.00")).grandTotal(new BigDecimal("1000.00")).build();

        // No active global or category rules exist
        when(commissionRuleRepository.findActiveGlobalRules(any())).thenReturn(List.of());
        when(commissionRuleRepository.findApplicableRules(any(), any())).thenReturn(List.of());

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(hinchStore));
        when(productRepository.findById(301)).thenReturn(Optional.of(hinchProduct));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(vendorSelectionService.getRankedCandidateVendors(any(), eq(deliveryAddress))).thenReturn(List.of(vendorStore));
        when(hinchStoreVendorInventoryRepository.atomicReserve(eq(301), eq(vendorStore.getStoreId()), eq(1))).thenReturn(1);

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).build();
        assertThatThrownBy(() -> orderService.createOrder(userId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active commission rule exists for categoryId 7 or global scope")
                .hasMessageContaining("Missing rule must throw.");
    }

    @Test
    @DisplayName("14. Regression: Seller payout ledger excludes GST and delivery charges")
    void testSellerPayoutExcludesGstAndDeliveryCharges() {
        int userId = 5;

        CartItemResponse ci = CartItemResponse.builder()
                .productId(201)
                .title("Vendor Cement 50kg")
                .quantity(2)
                .unit("BAG")
                .unitPrice(new BigDecimal("400.00"))
                .lineTotal(new BigDecimal("800.00"))
                .lineGst(new BigDecimal("144.00"))
                .build();

        CartResponse cart = CartResponse.builder()
                .storeId(10)
                .items(List.of(ci))
                .subtotal(new BigDecimal("800.00"))
                .grandTotal(new BigDecimal("1094.00")) // 800 + 144 GST + 100 Freight + 50 Crane
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("800.00"))
                .discount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("800.00"))
                .totalGst(new BigDecimal("144.00"))
                .freightCharge(new BigDecimal("100.00"))
                .craneUnloadingCharge(new BigDecimal("50.00"))
                .grandTotal(new BigDecimal("1094.00"))
                .build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(10)).thenReturn(Optional.of(vendorStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(vendorProduct1));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(vendorStore)).thenReturn("INV-VND-EXCL");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(5001);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(userId, OrderCreateRequest.builder().addressId(99).build());

        ArgumentCaptor<SellerPayoutLedger> ledgerCaptor = ArgumentCaptor.forClass(SellerPayoutLedger.class);
        verify(sellerPayoutLedgerRepository).save(ledgerCaptor.capture());
        SellerPayoutLedger savedLedger = ledgerCaptor.getValue();

        // Excludes GST (144.00), Freight (100.00), Crane (50.00). Gross is exactly item lineTotal (800.00)
        assertThat(savedLedger.getGrossAmount()).isEqualByComparingTo("800.00");
        assertThat(savedLedger.getTcsAmount()).isEqualByComparingTo("8.00"); // 1% TCS
        assertThat(savedLedger.getNetPayoutAmount()).isEqualByComparingTo("792.00");
        assertThat(savedLedger.getHinchKeeps()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("15. Regression: COD and online orders produce consistent settlement snapshots")
    void testCodAndOnlineOrders_ProduceConsistentSettlementSnapshots() {
        int userId = 5;

        CartItemResponse ci = CartItemResponse.builder()
                .productId(201)
                .title("Vendor Cement 50kg")
                .quantity(2)
                .unit("BAG")
                .unitPrice(new BigDecimal("400.00"))
                .lineTotal(new BigDecimal("800.00"))
                .lineGst(new BigDecimal("144.00"))
                .build();

        CartResponse cart = CartResponse.builder()
                .storeId(10)
                .items(List.of(ci))
                .subtotal(new BigDecimal("800.00"))
                .grandTotal(new BigDecimal("944.00"))
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("800.00"))
                .discount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("800.00"))
                .totalGst(new BigDecimal("144.00"))
                .grandTotal(new BigDecimal("944.00"))
                .build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(10)).thenReturn(Optional.of(vendorStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(vendorProduct1));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(vendorStore)).thenReturn("INV-VND-CONSISTENT");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(6001);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Create COD Order
        orderService.createOrder(userId, OrderCreateRequest.builder().addressId(99).paymentMethod("COD").build());
        ArgumentCaptor<OrderItem> codItemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, atLeastOnce()).save(codItemCaptor.capture());
        OrderItem codItem = codItemCaptor.getValue();

        // Create Online Order
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(6002);
            return o;
        });
        orderService.createOrder(userId, OrderCreateRequest.builder().addressId(99).paymentMethod("ONLINE").build());
        ArgumentCaptor<OrderItem> onlineItemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, atLeast(2)).save(onlineItemCaptor.capture());
        OrderItem onlineItem = onlineItemCaptor.getValue();

        // Both payment methods MUST produce bit-for-bit identical settlement snapshots
        assertThat(codItem.getCommissionRate()).isEqualByComparingTo(onlineItem.getCommissionRate());
        assertThat(codItem.getSellerPayable()).isEqualByComparingTo(onlineItem.getSellerPayable());
        assertThat(codItem.getHinchKeeps()).isEqualByComparingTo(onlineItem.getHinchKeeps());
        assertThat(codItem.getIsHinchStore()).isEqualTo(onlineItem.getIsHinchStore());
    }

    @Test
    @DisplayName("16. Regression: Ambiguous ownership (missing store and seller under HinchStore order) is rejected without applying commission rules")
    void testAmbiguousOwnership_MissingStoreAndSeller_ThrowsWithoutCommissionRules() {
        int userId = 5;
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        Product ambiguousProduct = Product.builder()
                .productId(401)
                .title("Orphaned Cement 50kg")
                .store(null)
                .seller(null)
                .stockQty(50)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .price(new BigDecimal("500.00"))
                .subcategory(mockSubcategory)
                .build();

        CartItemResponse ci = CartItemResponse.builder()
                .productId(401)
                .title("Orphaned Cement 50kg")
                .quantity(1)
                .unit("BAG")
                .unitPrice(new BigDecimal("500.00"))
                .lineTotal(new BigDecimal("500.00"))
                .lineGst(new BigDecimal("90.00"))
                .build();

        CartResponse cart = CartResponse.builder()
                .storeId(1)
                .items(List.of(ci))
                .subtotal(new BigDecimal("500.00"))
                .grandTotal(new BigDecimal("590.00"))
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("500.00"))
                .discount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("500.00"))
                .totalGst(new BigDecimal("90.00"))
                .grandTotal(new BigDecimal("590.00"))
                .build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(hinchStore));
        when(productRepository.findById(401)).thenReturn(Optional.of(ambiguousProduct));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(hinchStore)).thenReturn("INV-HINCH-AMBIG");
        when(vendorSelectionService.getRankedCandidateVendors(any(), eq(deliveryAddress))).thenReturn(List.of(vendorStoreWithoutSeller));
        when(hinchStoreVendorInventoryRepository.atomicReserve(eq(401), eq(vendorStoreWithoutSeller.getStoreId()), eq(1))).thenReturn(1);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(7001);
            return o;
        });

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).build();
        assertThatThrownBy(() -> orderService.createOrder(userId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot determine product ownership for item 'Orphaned Cement 50kg'")
                .hasMessageContaining("Ambiguous seller and store mapping.");

        // Must NEVER query commission rules for ambiguously mapped products
        org.mockito.Mockito.verifyNoInteractions(commissionRuleRepository);
    }

    @Test
    @DisplayName("17. Regression: Missing seller on vendor-owned product fails with clear error")
    void testVendorProduct_MissingSeller_FailsWithActionableError() {
        int userId = 5;

        Store vendorStoreNoSeller = Store.builder()
                .storeId(15)
                .seller(null)
                .name("Unassigned Vendor Store")
                .status(StoreStatus.ACTIVE)
                .build();

        Product productWithoutSeller = Product.builder()
                .productId(402)
                .title("Unassigned Sand 1 Ton")
                .store(vendorStoreNoSeller)
                .seller(null)
                .stockQty(20)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .price(new BigDecimal("2000.00"))
                .subcategory(mockSubcategory)
                .build();

        CartItemResponse ci = CartItemResponse.builder()
                .productId(402)
                .title("Unassigned Sand 1 Ton")
                .quantity(1)
                .unit("TON")
                .unitPrice(new BigDecimal("2000.00"))
                .lineTotal(new BigDecimal("2000.00"))
                .lineGst(new BigDecimal("360.00"))
                .build();

        CartResponse cart = CartResponse.builder()
                .storeId(15)
                .items(List.of(ci))
                .subtotal(new BigDecimal("2000.00"))
                .grandTotal(new BigDecimal("2360.00"))
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("2000.00"))
                .discount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("2000.00"))
                .totalGst(new BigDecimal("360.00"))
                .grandTotal(new BigDecimal("2360.00"))
                .build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(15)).thenReturn(Optional.of(vendorStoreNoSeller));
        when(productRepository.findById(402)).thenReturn(Optional.of(productWithoutSeller));
        when(productRepository.findByIdForStockUpdate(402)).thenReturn(Optional.of(productWithoutSeller));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(vendorStoreNoSeller)).thenReturn("INV-VND-NOSELLER");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(7002);
            return o;
        });

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).build();
        assertThatThrownBy(() -> orderService.createOrder(userId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has no associated seller. Seller ID is required for vendor products.");

        org.mockito.Mockito.verifyNoInteractions(commissionRuleRepository);
    }

    @Test
    @DisplayName("18. Regression: Genuine HinchStore consignment product with platform seller #1 applies configured holding fee")
    void testGenuineHinchStoreConsignment_WithPlatformSeller1_AppliesHoldingFee() {
        int userId = 5;
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        Seller platformAdminSeller = Seller.builder()
                .sellerId(1)
                .name("HinchMart Admin")
                .build();

        Product platformAdminProduct = Product.builder()
                .productId(303)
                .title("Tata Tiscon 550D Rebar 16mm")
                .store(hinchStore)
                .seller(platformAdminSeller)
                .stockQty(150)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .price(new BigDecimal("1200.00"))
                .subcategory(mockSubcategory)
                .build();

        CommissionRule rule = CommissionRule.builder()
                .ruleId(1)
                .rate(new BigDecimal("7.50"))
                .isActive(true)
                .build();

        CartItemResponse ci = CartItemResponse.builder()
                .productId(303)
                .title("Tata Tiscon 550D Rebar 16mm")
                .quantity(1)
                .unit("TON")
                .unitPrice(new BigDecimal("1200.00"))
                .lineTotal(new BigDecimal("1200.00"))
                .lineGst(new BigDecimal("216.00"))
                .build();

        CartResponse cart = CartResponse.builder()
                .storeId(1)
                .items(List.of(ci))
                .subtotal(new BigDecimal("1200.00"))
                .grandTotal(new BigDecimal("1416.00"))
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("1200.00"))
                .discount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("1200.00"))
                .totalGst(new BigDecimal("216.00"))
                .grandTotal(new BigDecimal("1416.00"))
                .build();

        when(commissionRuleRepository.findActiveGlobalRules(any())).thenReturn(List.of(rule));
        when(commissionRuleRepository.findApplicableRules(any(), any())).thenReturn(List.of(rule));

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(hinchStore));
        when(productRepository.findById(303)).thenReturn(Optional.of(platformAdminProduct));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 99)).thenReturn(Optional.of(deliveryAddress));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(mockCustomer));
        when(checkoutService.previewCheckout(eq(userId), any())).thenReturn(preview);
        when(vendorSelectionService.getRankedCandidateVendors(any(), eq(deliveryAddress))).thenReturn(List.of(vendorStore));
        when(hinchStoreVendorInventoryRepository.atomicReserve(eq(303), eq(vendorStore.getStoreId()), eq(1))).thenReturn(1);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(hinchStore)).thenReturn("INV-HINCH-GENUINE");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(7003);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(userId, OrderCreateRequest.builder().addressId(99).build());

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, atLeastOnce()).save(itemCaptor.capture());
        OrderItem savedItem = itemCaptor.getValue();

        // 7.50% holding fee applied: 1200 * 0.075 = 90.00
        assertThat(savedItem.getCommissionRate()).isEqualByComparingTo("7.50");
        assertThat(savedItem.getSellerPayable()).isEqualByComparingTo("90.00");
        assertThat(savedItem.getHinchKeeps()).isEqualByComparingTo("1110.00");
        assertThat(savedItem.getSeller()).isEqualTo(vendorSeller);
    }
}
