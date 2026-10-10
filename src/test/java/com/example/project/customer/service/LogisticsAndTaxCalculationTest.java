package com.example.project.customer.service;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.*;
import com.example.project.customer.dto.tax.TaxCalculationResult;
import com.example.project.customer.dto.tax.TaxableItemInput;
import com.example.project.customer.dto.tax.TaxableItemResult;
import com.example.project.customer.entity.*;
import com.example.project.customer.repository.*;
import com.example.project.customer.security.FirebaseUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LogisticsAndTaxCalculationTest {

    private TaxCalculationServiceImpl taxCalculationService;

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
    @Mock private UserContextUtil userContextUtil;
    @Mock private SellerContextUtil sellerContextUtil;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        taxCalculationService = new TaxCalculationServiceImpl();

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
                userContextUtil,
                sellerContextUtil
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsAdmin() {
        FirebaseUserPrincipal principal = FirebaseUserPrincipal.builder()
                .firebaseUid("admin-uid")
                .internalUserId(999)
                .email("admin@hinchmart.com")
                .role(Role.ADMIN)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private void authenticateAsCustomer(int customerId) {
        FirebaseUserPrincipal principal = FirebaseUserPrincipal.builder()
                .firebaseUid("cust-uid-" + customerId)
                .internalUserId(customerId)
                .email("customer" + customerId + "@example.com")
                .role(Role.CUSTOMER)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        when(userContextUtil.getOptionalCurrentUserId()).thenReturn(customerId);
    }

    // ==========================================
    // 1. TAX CALCULATION SERVICE TESTS
    // ==========================================

    @Test
    @DisplayName("Tax Calculation: Intra-state 18% splits equally into 9% CGST and 9% SGST with 0% IGST")
    void testIntraStateTaxCalculation() {
        TaxableItemInput item = TaxableItemInput.builder()
                .productId(1)
                .title("TMT Rebar 500D")
                .hsnCode("721420")
                .quantity(10)
                .unitPrice(new BigDecimal("100.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .gstRate(new BigDecimal("18.00"))
                .build();

        boolean isIntra = taxCalculationService.isIntraState("Telangana", "Telangana");
        assertThat(isIntra).isTrue();

        TaxCalculationResult result = taxCalculationService.calculateTaxes(
                List.of(item),
                BigDecimal.ZERO,
                isIntra,
                null,
                null
        );

        assertThat(result.isIntraState()).isTrue();
        assertThat(result.getTaxableAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.getCgst()).isEqualByComparingTo("90.00");
        assertThat(result.getSgst()).isEqualByComparingTo("90.00");
        assertThat(result.getIgst()).isEqualByComparingTo("0.00");
        assertThat(result.getTotalGst()).isEqualByComparingTo("180.00");
        assertThat(result.getItemResults()).hasSize(1);

        TaxableItemResult itemResult = result.getItemResults().get(0);
        assertThat(itemResult.getLineCgst()).isEqualByComparingTo("90.00");
        assertThat(itemResult.getLineSgst()).isEqualByComparingTo("90.00");
        assertThat(itemResult.getLineIgst()).isEqualByComparingTo("0.00");
        assertThat(itemResult.getLineGst()).isEqualByComparingTo("180.00");
    }

    @Test
    @DisplayName("Tax Calculation: Inter-state 18% assigns to 18% IGST with 0% CGST and 0% SGST")
    void testInterStateTaxCalculation() {
        TaxableItemInput item = TaxableItemInput.builder()
                .productId(1)
                .title("TMT Rebar 500D")
                .hsnCode("721420")
                .quantity(10)
                .unitPrice(new BigDecimal("100.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .gstRate(new BigDecimal("18.00"))
                .build();

        boolean isIntra = taxCalculationService.isIntraState("Telangana", "Karnataka");
        assertThat(isIntra).isFalse();

        TaxCalculationResult result = taxCalculationService.calculateTaxes(
                List.of(item),
                BigDecimal.ZERO,
                isIntra,
                null,
                null
        );

        assertThat(result.isIntraState()).isFalse();
        assertThat(result.getTaxableAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.getCgst()).isEqualByComparingTo("0.00");
        assertThat(result.getSgst()).isEqualByComparingTo("0.00");
        assertThat(result.getIgst()).isEqualByComparingTo("180.00");
        assertThat(result.getTotalGst()).isEqualByComparingTo("180.00");
    }

    @Test
    @DisplayName("Tax Calculation: Multi-rate basket (5%, 18%, 28%) accurately calculates each line and reconciles totals")
    void testMultiRateBasketCalculation() {
        TaxableItemInput sand = TaxableItemInput.builder()
                .productId(1)
                .title("River Sand")
                .quantity(1)
                .unitPrice(new BigDecimal("500.00"))
                .lineTotal(new BigDecimal("500.00"))
                .gstRate(new BigDecimal("5.00"))
                .build();

        TaxableItemInput steel = TaxableItemInput.builder()
                .productId(2)
                .title("Steel Rods")
                .quantity(1)
                .unitPrice(new BigDecimal("1000.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .gstRate(new BigDecimal("18.00"))
                .build();

        TaxableItemInput cement = TaxableItemInput.builder()
                .productId(3)
                .title("OPC 53 Cement")
                .quantity(1)
                .unitPrice(new BigDecimal("2000.00"))
                .lineTotal(new BigDecimal("2000.00"))
                .gstRate(new BigDecimal("28.00"))
                .build();

        TaxCalculationResult result = taxCalculationService.calculateTaxes(
                List.of(sand, steel, cement),
                BigDecimal.ZERO,
                true,
                null,
                null
        );

        assertThat(result.getTaxableAmount()).isEqualByComparingTo("3500.00");
        // Sand: 500 * 5% = 25.00 (CGST 12.50, SGST 12.50)
        // Steel: 1000 * 18% = 180.00 (CGST 90.00, SGST 90.00)
        // Cement: 2000 * 28% = 560.00 (CGST 280.00, SGST 280.00)
        // Total GST = 25 + 180 + 560 = 765.00
        assertThat(result.getTotalGst()).isEqualByComparingTo("765.00");
        assertThat(result.getCgst()).isEqualByComparingTo("382.50");
        assertThat(result.getSgst()).isEqualByComparingTo("382.50");
        assertThat(result.getIgst()).isEqualByComparingTo("0.00");

        // Verify line sum reconciliation
        BigDecimal sumItemGst = result.getItemResults().stream()
                .map(TaxableItemResult::getLineGst)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumItemGst).isEqualByComparingTo(result.getTotalGst());
    }

    @Test
    @DisplayName("Tax Calculation: Coupon discount is proportionally allocated across line items with zero rounding divergence")
    void testProportionalCouponDiscountAllocation() {
        TaxableItemInput item1 = TaxableItemInput.builder()
                .productId(1).quantity(1).unitPrice(new BigDecimal("100.00")).lineTotal(new BigDecimal("100.00"))
                .gstRate(new BigDecimal("18.00")).build();
        TaxableItemInput item2 = TaxableItemInput.builder()
                .productId(2).quantity(1).unitPrice(new BigDecimal("100.00")).lineTotal(new BigDecimal("100.00"))
                .gstRate(new BigDecimal("18.00")).build();
        TaxableItemInput item3 = TaxableItemInput.builder()
                .productId(3).quantity(1).unitPrice(new BigDecimal("100.00")).lineTotal(new BigDecimal("100.00"))
                .gstRate(new BigDecimal("18.00")).build();

        TaxCalculationResult result = taxCalculationService.calculateTaxes(
                List.of(item1, item2, item3),
                new BigDecimal("10.00"),
                true,
                null,
                null
        );

        BigDecimal sumDiscounts = result.getItemResults().stream()
                .map(TaxableItemResult::getAllocatedDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumDiscounts).isEqualByComparingTo("10.00");

        BigDecimal sumTaxable = result.getItemResults().stream()
                .map(TaxableItemResult::getLineTaxable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumTaxable).isEqualByComparingTo("290.00");
        assertThat(result.getTaxableAmount()).isEqualByComparingTo("290.00");

        BigDecimal sumTax = result.getItemResults().stream()
                .map(TaxableItemResult::getLineGst)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumTax).isEqualByComparingTo(result.getTotalGst());
    }

    @Test
    @DisplayName("Tax Calculation: Missing or blank buyer state in isIntraState throws IllegalArgumentException")
    void testMissingBuyerStateThrowsException() {
        assertThatThrownBy(() -> taxCalculationService.isIntraState("Telangana", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Delivery address state is required");

        assertThatThrownBy(() -> taxCalculationService.isIntraState("Telangana", "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Tax Calculation: Missing or invalid product gstRate throws IllegalStateException")
    void testMissingOrInvalidGstRateThrowsException() {
        TaxableItemInput itemNullRate = TaxableItemInput.builder()
                .productId(1).lineTotal(new BigDecimal("100.00")).gstRate(null).build();

        assertThatThrownBy(() -> taxCalculationService.calculateTaxes(
                List.of(itemNullRate), BigDecimal.ZERO, true, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing or invalid GST tax rate");

        TaxableItemInput itemNegativeRate = TaxableItemInput.builder()
                .productId(1).lineTotal(new BigDecimal("100.00")).gstRate(new BigDecimal("-5.00")).build();

        assertThatThrownBy(() -> taxCalculationService.calculateTaxes(
                List.of(itemNegativeRate), BigDecimal.ZERO, true, null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Tax Calculation: Tax-inclusive RFQ amount decomposes cleanly into taxable base and intra/inter tax")
    void testCalculateTaxInclusive() {
        TaxCalculationResult intraResult = taxCalculationService.calculateTaxInclusive(
                new BigDecimal("1180.00"),
                new BigDecimal("18.00"),
                true
        );
        assertThat(intraResult.isIntraState()).isTrue();
        assertThat(intraResult.getTaxableAmount()).isEqualByComparingTo("1000.00");
        assertThat(intraResult.getCgst()).isEqualByComparingTo("90.00");
        assertThat(intraResult.getSgst()).isEqualByComparingTo("90.00");
        assertThat(intraResult.getIgst()).isEqualByComparingTo("0.00");
        assertThat(intraResult.getTotalGst()).isEqualByComparingTo("180.00");

        TaxCalculationResult interResult = taxCalculationService.calculateTaxInclusive(
                new BigDecimal("1180.00"),
                new BigDecimal("18.00"),
                false
        );
        assertThat(interResult.isIntraState()).isFalse();
        assertThat(interResult.getTaxableAmount()).isEqualByComparingTo("1000.00");
        assertThat(interResult.getCgst()).isEqualByComparingTo("0.00");
        assertThat(interResult.getSgst()).isEqualByComparingTo("0.00");
        assertThat(interResult.getIgst()).isEqualByComparingTo("180.00");
        assertThat(interResult.getTotalGst()).isEqualByComparingTo("180.00");
    }

    // ==========================================
    // 2. LOGISTICS DATA DEFECT & ORDER LIFECYCLE TESTS
    // ==========================================

    @Test
    @DisplayName("Logistics: Order creation sets carrierName, driverName, vehicleNumber, trackingNumber, and estimatedDelivery to NULL")
    void testCreateOrderSetsLogisticsToNull() {
        Customer customer = Customer.builder().customerId(1).phone("9876543210").name("John Doe").build();
        Address address = Address.builder()
                .id(10)
                .customer(customer)
                .siteName("Site A")
                .recipientName("John Doe")
                .phone("9876543210")
                .addressLine1("Line 1")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("500081")
                .country("India")
                .build();

        Product product = Product.builder()
                .productId(101)
                .title("Steel Bar")
                .price(new BigDecimal("100.00"))
                .stockQty(50)
                .gstRate(new BigDecimal("18.00"))
                .hsnCode("721420")
                .build();

        CartItemResponse cartItemResponse = CartItemResponse.builder()
                .cartItemId(1)
                .productId(101)
                .title("Steel Bar")
                .quantity(5)
                .unitPrice(new BigDecimal("100.00"))
                .lineTotal(new BigDecimal("500.00"))
                .gstRate(new BigDecimal("18.00"))
                .hsnCode("721420")
                .lineGst(new BigDecimal("90.00"))
                .build();

        CartResponse cartResponse = CartResponse.builder()
                .cartId(1)
                .items(List.of(cartItemResponse))
                .subtotal(new BigDecimal("500.00"))
                .totalGst(new BigDecimal("90.00"))
                .couponDiscount(BigDecimal.ZERO)
                .deliveryCharge(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("590.00"))
                .build();

        Store store = Store.builder().storeId(1).name("HinchStore").build();

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomer_CustomerIdAndId(1, 10)).thenReturn(Optional.of(address));
        when(cartService.getCart(1)).thenReturn(cartResponse);
        when(productRepository.findById(101)).thenReturn(Optional.of(product));
        when(productRepository.findByIdForStockUpdate(101)).thenReturn(Optional.of(product));
        when(storeRepository.findById(any())).thenReturn(Optional.of(store));
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(any())).thenReturn("INV-2026-0001");

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("500.00"))
                .taxableAmount(new BigDecimal("500.00"))
                .discount(BigDecimal.ZERO)
                .cgst(new BigDecimal("45.00"))
                .sgst(new BigDecimal("45.00"))
                .igst(BigDecimal.ZERO)
                .totalGst(new BigDecimal("90.00"))
                .freightCharge(BigDecimal.ZERO)
                .craneUnloadingCharge(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("590.00"))
                .build();
        when(checkoutService.previewCheckout(eq(1), any())).thenReturn(preview);

        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setOrderId(1001);
            return o;
        });

        OrderCreateRequest request = OrderCreateRequest.builder()
                .addressId(10)
                .paymentMethod("ONLINE")
                .build();

        OrderResponse response = orderService.createOrder(1, request);

        assertThat(response).isNotNull();
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getCarrierName()).isNull();
        assertThat(savedOrder.getDriverName()).isNull();
        assertThat(savedOrder.getVehicleNumber()).isNull();
        assertThat(savedOrder.getTrackingNumber()).isNull();
        assertThat(savedOrder.getEstimatedDelivery()).isNull();
        assertThat(savedOrder.getOrderStatus()).isEqualTo("PLACED");
    }

    @Test
    @DisplayName("Logistics: getOrderTracking returns null logistics details and empty checkpoints for unassigned order")
    void testGetOrderTrackingWithNullLogistics() {
        authenticateAsCustomer(1);
        Customer customer = Customer.builder().customerId(1).phone("9876543210").build();
        Order order = Order.builder()
                .orderId(1001)
                .customer(customer)
                .orderStatus("PLACED")
                .carrierName(null)
                .driverName(null)
                .vehicleNumber(null)
                .trackingNumber(null)
                .estimatedDelivery(null)
                .build();

        when(orderRepository.findById(1001)).thenReturn(Optional.of(order));

        OrderTrackingResponse tracking = orderService.getOrderTracking(1001);

        assertThat(tracking).isNotNull();
        assertThat(tracking.getCarrierName()).isNull();
        assertThat(tracking.getDriverName()).isNull();
        assertThat(tracking.getVehicleNumber()).isNull();
        assertThat(tracking.getTrackingNumber()).isNull();
        assertThat(tracking.getEstimatedDelivery()).isNull();
        assertThat(tracking.getCurrentStatus()).isEqualTo("PLACED");
        assertThat(tracking.getCheckpoints()).isEmpty();
    }

    @Test
    @DisplayName("Logistics: Genuine dispatch update successfully assigns carrier, vehicle, driver, and tracking number")
    void testUpdateOrderStatusWithGenuineDispatchDetails() {
        authenticateAsAdmin();
        Customer customer = Customer.builder().customerId(1).phone("9876543210").build();
        Order order = Order.builder()
                .orderId(1001)
                .customer(customer)
                .orderStatus("PLACED")
                .paymentStatus("PAID")
                .carrierName(null)
                .driverName(null)
                .vehicleNumber(null)
                .trackingNumber(null)
                .build();

        when(orderRepository.findById(1001)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse updated = orderService.updateOrderStatus(
                1001,
                "DISPATCHED",
                "Central Hub",
                "Order picked up by carrier",
                "BlueDart Logistics",
                "TS09AB1234",
                "Ramesh Kumar",
                "BLU889900",
                "2026-10-15T18:00:00"
        );

        assertThat(updated.getOrderStatus()).isEqualTo("DISPATCHED");
        assertThat(order.getCarrierName()).isEqualTo("BlueDart Logistics");
        assertThat(order.getVehicleNumber()).isEqualTo("TS09AB1234");
        assertThat(order.getDriverName()).isEqualTo("Ramesh Kumar");
        assertThat(order.getTrackingNumber()).isEqualTo("BLU889900");
        assertThat(order.getEstimatedDelivery()).isEqualTo(LocalDateTime.parse("2026-10-15T18:00:00"));

        OrderTrackingResponse tracking = orderService.getOrderTracking(1001);
        assertThat(tracking.getCarrierName()).isEqualTo("BlueDart Logistics");
        assertThat(tracking.getVehicleNumber()).isEqualTo("TS09AB1234");
        assertThat(tracking.getDriverName()).isEqualTo("Ramesh Kumar");
        assertThat(tracking.getTrackingNumber()).isEqualTo("BLU889900");
    }

    // ==========================================
    // 3. INVOICE AND HSN ACCURACY TESTS
    // ==========================================

    @Test
    @DisplayName("Invoice: getOrderInvoice displays product-specific HSN and dynamic Place of Supply")
    void testGetOrderInvoiceHsnAndPlaceOfSupply() {
        authenticateAsCustomer(1);
        Customer customer = Customer.builder().customerId(1).name("Pavan Kumar").phone("9876543210").build();
        Address address = Address.builder()
                .id(10)
                .customer(customer)
                .recipientName("Pavan Kumar")
                .phone("9876543210")
                .addressLine1("Plot 42")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("500081")
                .build();

        Product product = Product.builder()
                .productId(201)
                .title("UltraTech Cement 53")
                .hsnCode("252329")
                .price(new BigDecimal("380.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .orderItemId(1)
                .productId(201)
                .title("UltraTech Cement 53")
                .quantity(10)
                .unitPrice(new BigDecimal("380.00"))
                .build();

        Order order = Order.builder()
                .orderId(1001)
                .customer(customer)
                .deliveryLocation("Plot 42, Hyderabad, Telangana")
                .items(List.of(item))
                .subtotal(new BigDecimal("3800.00"))
                .totalAmount(new BigDecimal("4864.00"))
                .cgst(new BigDecimal("532.00"))
                .sgst(new BigDecimal("532.00"))
                .igst(BigDecimal.ZERO)
                .totalGst(new BigDecimal("1064.00"))
                .freightCharge(BigDecimal.ZERO)
                .craneUnloadingCharge(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1001)).thenReturn(Optional.of(order));
        when(productRepository.findById(201)).thenReturn(Optional.of(product));

        InvoiceResponse invoice = orderService.getOrderInvoice(1001);

        assertThat(invoice).isNotNull();
        assertThat(invoice.getPlaceOfSupply()).contains("Telangana");
        assertThat(invoice.getItems()).hasSize(1);
        assertThat(invoice.getItems().get(0).getHsnCode()).isEqualTo("252329");
        assertThat(invoice.getCgst()).isEqualByComparingTo("532.00");
        assertThat(invoice.getSgst()).isEqualByComparingTo("532.00");
        assertThat(invoice.getIgst()).isEqualByComparingTo("0.00");
    }
}
