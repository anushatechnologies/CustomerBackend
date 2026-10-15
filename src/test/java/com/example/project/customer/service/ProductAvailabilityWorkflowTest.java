package com.example.project.customer.service;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.dto.*;
import com.example.project.customer.entity.*;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductAvailabilityWorkflowTest {

    @Mock private ProductRepository productRepository;
    @Mock private HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private CouponService couponService;
    @Mock private AddressRepository addressRepository;
    @Mock private TaxCalculationService taxCalculationService;

    private PlatformMarketplaceProperties platformMarketplaceProperties;
    private ProductAvailabilityServiceImpl availabilityService;
    private CartServiceImpl cartService;
    private CheckoutServiceImpl checkoutService;
    private ProductServiceImpl productService;

    private Store platformStore;
    private Store vendorStore1;
    private Store vendorStore2;

    private Product vendorProduct;
    private Product hinchProductWithStock;
    private Product hinchProductNoRows;

    @BeforeEach
    void setUp() {
        platformMarketplaceProperties = new PlatformMarketplaceProperties();
        platformMarketplaceProperties.setPlatformStoreId(1);
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        availabilityService = new ProductAvailabilityServiceImpl(
                productRepository, hinchStoreVendorInventoryRepository, platformMarketplaceProperties
        );

        cartService = new CartServiceImpl(
                cartRepository, cartItemRepository, productRepository, storeRepository,
                couponService, taxCalculationService
        );
        cartService.setProductAvailabilityService(availabilityService);

        checkoutService = new CheckoutServiceImpl(
                cartService, addressRepository, storeRepository, taxCalculationService
        );

        productService = new ProductServiceImpl(
                productRepository, null, null, null, null, null, null
        );
        productService.setProductAvailabilityService(availabilityService);

        platformStore = Store.builder().storeId(1).name("HinchStore").status(StoreStatus.ACTIVE).build();
        vendorStore1 = Store.builder().storeId(10).name("Vendor Store 1").status(StoreStatus.ACTIVE).build();
        vendorStore2 = Store.builder().storeId(20).name("Vendor Store 2").status(StoreStatus.ACTIVE).build();

        vendorProduct = Product.builder()
                .productId(101)
                .title("Vendor Steel Bar")
                .store(vendorStore1)
                .stockQty(15)
                .price(new BigDecimal("100.00"))
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        hinchProductWithStock = Product.builder()
                .productId(201)
                .title("Platform Cement 50kg")
                .store(platformStore)
                .stockQty(999) // Catalog stock should be ignored for HinchStore
                .price(new BigDecimal("350.00"))
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        hinchProductNoRows = Product.builder()
                .productId(202)
                .title("Platform Red Bricks")
                .store(platformStore)
                .stockQty(500)
                .price(new BigDecimal("10.00"))
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        when(productRepository.findById(101)).thenReturn(Optional.of(vendorProduct));
        when(productRepository.findById(201)).thenReturn(Optional.of(hinchProductWithStock));
        when(productRepository.findById(202)).thenReturn(Optional.of(hinchProductNoRows));
    }

    @Test
    @DisplayName("Vendor-own: available == stock_qty; add over limit rejected, at limit works")
    void testVendorOwnAvailabilityAndCartLimits() {
        ProductAvailability avail = availabilityService.getAvailability(vendorProduct);
        assertThat(avail.getAvailableQty()).isEqualTo(15);
        assertThat(avail.isInStock()).isTrue();
        assertThat(avail.getReason()).isNull();

        Cart cart = Cart.builder().cartId(1).store(vendorStore1).build();
        when(cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(999)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(1)).thenReturn(new ArrayList<>());
        when(cartItemRepository.findAllByCart_CartIdAndProduct_ProductId(1, 101)).thenReturn(new ArrayList<>());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        // Adding quantity 16 (over limit of 15) must be rejected with available count in message
        CartItemRequest overLimitReq = CartItemRequest.builder().productId(101).quantity(16).build();
        assertThatThrownBy(() -> cartService.addItem(999, overLimitReq))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("exceeds available stock (available: 15)");

        // Adding quantity 15 (at limit) works
        when(taxCalculationService.calculateTaxes(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(com.example.project.customer.dto.tax.TaxCalculationResult.builder()
                        .subtotal(new BigDecimal("1500"))
                        .grandTotal(new BigDecimal("1500"))
                        .totalGst(BigDecimal.ZERO)
                        .itemResults(Collections.emptyList())
                        .build());

        CartItemRequest atLimitReq = CartItemRequest.builder().productId(101).quantity(15).build();
        CartResponse response = cartService.addItem(999, atLimitReq);
        assertThat(response).isNotNull();
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("HinchStore: available == SUM across vendors minus reserved; flag off gives CHECKOUT_DISABLED")
    void testHinchStoreAvailabilitySumAcrossVendors() {
        // Vendor 1: qty 50, reserved 10 -> 40
        // Vendor 2: qty 30, reserved 5  -> 25
        // SUM across vendors minus reserved = 65, count = 2
        List<Object[]> rows = Collections.singletonList(new Object[]{201, 65, 2L});
        when(hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(List.of(201)))
                .thenReturn(rows);

        // 1. With checkout flag disabled (default):
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);
        ProductAvailability availFlagOff = availabilityService.getAvailability(hinchProductWithStock);
        assertThat(availFlagOff.getAvailableQty()).isEqualTo(65);
        assertThat(availFlagOff.isInStock()).isFalse();
        assertThat(availFlagOff.getReason()).isEqualTo("CHECKOUT_DISABLED");

        // 2. With checkout flag enabled:
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);
        ProductAvailability availFlagOn = availabilityService.getAvailability(hinchProductWithStock);
        assertThat(availFlagOn.getAvailableQty()).isEqualTo(65);
        assertThat(availFlagOn.isInStock()).isTrue();
        assertThat(availFlagOn.getReason()).isNull();
    }

    @Test
    @DisplayName("HinchStore with no inventory rows -> UNAVAILABLE_NO_VENDOR_STOCK")
    void testHinchStoreNoInventoryRowsGivesExplicitReason() {
        // Query returns empty list for product 202
        when(hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(List.of(202)))
                .thenReturn(Collections.emptyList());

        ProductAvailability avail = availabilityService.getAvailability(hinchProductNoRows);
        assertThat(avail.getAvailableQty()).isEqualTo(0);
        assertThat(avail.isInStock()).isFalse();
        assertThat(avail.getReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");
    }

    @Test
    @DisplayName("Cart add/update errors show the real reason: CHECKOUT_DISABLED or UNAVAILABLE_NO_VENDOR_STOCK")
    void testCartAddAndUpdateErrorsShowRealReason() {
        // 1. HinchStore product with no inventory rows -> UNAVAILABLE_NO_VENDOR_STOCK
        when(hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(List.of(202)))
                .thenReturn(Collections.emptyList());

        CartItemRequest noRowsReq = CartItemRequest.builder().productId(202).quantity(1).build();
        assertThatThrownBy(() -> cartService.addItem(999, noRowsReq))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("UNAVAILABLE_NO_VENDOR_STOCK");

        // 2. HinchStore product with stock, but checkout disabled -> CHECKOUT_DISABLED
        List<Object[]> rows = Collections.singletonList(new Object[]{201, 65, 1L});
        when(hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(List.of(201)))
                .thenReturn(rows);
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        CartItemRequest checkoutDisabledReq = CartItemRequest.builder().productId(201).quantity(2).build();
        assertThatThrownBy(() -> cartService.addItem(999, checkoutDisabledReq))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("CHECKOUT_DISABLED");

        // 3. Cart update item with UNAVAILABLE_NO_VENDOR_STOCK
        Cart cart = Cart.builder().cartId(1).store(platformStore).build();
        CartItem item = CartItem.builder().cartItemId(10).cart(cart).product(hinchProductNoRows).quantity(1).build();
        when(cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(999)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartIdAndCartItemId(1, 10)).thenReturn(Optional.of(item));

        com.example.project.customer.dto.CartItemUpdateRequest updateReq = com.example.project.customer.dto.CartItemUpdateRequest.builder().quantity(5).build();
        assertThatThrownBy(() -> cartService.updateItem(999, 10, updateReq))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("UNAVAILABLE_NO_VENDOR_STOCK");
    }

    @Test
    @DisplayName("Cart view and preview flag unavailable items without deleting them; preview states order cannot be placed")
    void testCartViewAndPreviewFlagUnavailableItemsWithoutDeleting() {
        Cart cart = Cart.builder().cartId(5).store(platformStore).build();
        CartItem item1 = CartItem.builder().cartItemId(51).cart(cart).product(hinchProductWithStock).quantity(10).build();
        CartItem item2 = CartItem.builder().cartItemId(52).cart(cart).product(hinchProductNoRows).quantity(5).build();

        when(cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(777)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(5)).thenReturn(List.of(item1, item2));

        // hinchProductWithStock has 65 available (flag disabled -> CHECKOUT_DISABLED)
        // hinchProductNoRows has 0 rows -> UNAVAILABLE_NO_VENDOR_STOCK
        List<Object[]> rows1 = Collections.singletonList(new Object[]{201, 65, 1L});
        when(hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(any()))
                .thenReturn(rows1);

        when(taxCalculationService.calculateTaxes(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(com.example.project.customer.dto.tax.TaxCalculationResult.builder()
                        .subtotal(new BigDecimal("1000"))
                        .grandTotal(new BigDecimal("1180"))
                        .totalGst(new BigDecimal("180"))
                        .itemResults(List.of(
                                com.example.project.customer.dto.tax.TaxableItemResult.builder().productId(201).unitPrice(new BigDecimal("100")).build(),
                                com.example.project.customer.dto.tax.TaxableItemResult.builder().productId(202).unitPrice(new BigDecimal("80")).build()
                        ))
                        .build());

        Address address = Address.builder().id(88).state("Telangana").build();
        when(addressRepository.findByCustomer_CustomerIdAndId(777, 88)).thenReturn(Optional.of(address));

        // 1. Cart View: flags unavailable items, items list still has size 2 (no deletion)
        CartResponse cartResponse = cartService.getCart(777);
        assertThat(cartResponse.getItems()).hasSize(2);
        verify(cartItemRepository, never()).delete(any());

        CartItemResponse resp1 = cartResponse.getItems().get(0);
        assertThat(resp1.getProductId()).isEqualTo(201);
        assertThat(resp1.getAvailableQty()).isEqualTo(65);
        assertThat(resp1.getReason()).isEqualTo("CHECKOUT_DISABLED");

        CartItemResponse resp2 = cartResponse.getItems().get(1);
        assertThat(resp2.getProductId()).isEqualTo(202);
        assertThat(resp2.getAvailableQty()).isEqualTo(0);
        assertThat(resp2.getInStock()).isFalse();
        assertThat(resp2.getReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");
        assertThat(cartResponse.getCanCheckout()).isFalse();

        // 2. Checkout Preview: states order cannot be placed while any item is unavailable
        CheckoutPreviewRequest previewReq = CheckoutPreviewRequest.builder().addressId(88).build();
        CheckoutPreviewResponse previewResp = checkoutService.previewCheckout(777, previewReq);

        assertThat(previewResp.getItems()).hasSize(2);
        assertThat(previewResp.getCanPlaceOrder()).isFalse();
        assertThat(previewResp.getOrderBlockReason())
                .isEqualTo("Order cannot be placed while any item is unavailable.");
    }

    @Test
    @DisplayName("List, detail, cart and preview return the same number for the same product")
    void testConsistencyAcrossListDetailCartAndPreview() {
        List<Object[]> rows2 = Collections.singletonList(new Object[]{201, 42, 2L});
        when(hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(any()))
                .thenReturn(rows2);

        // 1. Availability service direct
        ProductAvailability avail = availabilityService.getAvailability(hinchProductWithStock);
        assertThat(avail.getAvailableQty()).isEqualTo(42);

        // 2. Product detail
        when(productRepository.findByProductIdAndApprovalStatusAndActive(201, ApprovalStatus.APPROVED, true))
                .thenReturn(Optional.of(hinchProductWithStock));
        ProductResponse detailResp = productService.getById(201);
        assertThat(detailResp.getStockQty()).isEqualTo(42);
        assertThat(detailResp.getAvailableQty()).isEqualTo(42);

        // 3. Product list
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(hinchProductWithStock)));
        ApiResponse<List<ProductResponse>> listResp = productService.getAll(null, null, null, null, null, null, null, "newest", 1, 20);
        assertThat(listResp.getData().get(0).getStockQty()).isEqualTo(42);
        assertThat(listResp.getData().get(0).getAvailableQty()).isEqualTo(42);

        // 4. Cart View
        Cart cart = Cart.builder().cartId(8).store(platformStore).build();
        CartItem item = CartItem.builder().cartItemId(81).cart(cart).product(hinchProductWithStock).quantity(2).build();
        when(cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(123)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(8)).thenReturn(List.of(item));
        when(taxCalculationService.calculateTaxes(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(com.example.project.customer.dto.tax.TaxCalculationResult.builder()
                        .subtotal(new BigDecimal("700"))
                        .grandTotal(new BigDecimal("700"))
                        .itemResults(List.of(com.example.project.customer.dto.tax.TaxableItemResult.builder().productId(201).unitPrice(BigDecimal.ZERO).build()))
                        .build());

        CartResponse cartResp = cartService.getCart(123);
        assertThat(cartResp.getItems().get(0).getAvailableQty()).isEqualTo(42);

        // 5. Preview
        Address address = Address.builder().id(1).state("Telangana").build();
        when(addressRepository.findByCustomer_CustomerIdAndId(123, 1)).thenReturn(Optional.of(address));
        CheckoutPreviewResponse preview = checkoutService.previewCheckout(123, CheckoutPreviewRequest.builder().addressId(1).build());
        assertThat(preview.getItems().get(0).getAvailableQty()).isEqualTo(42);
    }

    @Test
    @DisplayName("Query count stays flat for a 20-item cart")
    void testQueryCountStaysFlatForTwentyItemCart() {
        List<Product> products = new ArrayList<>();
        List<CartItem> cartItems = new ArrayList<>();
        Cart cart = Cart.builder().cartId(20).store(platformStore).build();

        for (int i = 1; i <= 20; i++) {
            Product p = Product.builder()
                    .productId(1000 + i)
                    .title("Platform Item #" + i)
                    .store(platformStore)
                    .stockQty(10)
                    .price(new BigDecimal("50.00"))
                    .active(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .build();
            products.add(p);
            cartItems.add(CartItem.builder().cartItemId(i).cart(cart).product(p).quantity(1).build());
        }

        when(cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(555)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartId(20)).thenReturn(cartItems);

        List<Object[]> mockStockRows = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            mockStockRows.add(new Object[]{1000 + i, 100, 1L});
        }
        when(hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(any()))
                .thenReturn(mockStockRows);

        when(taxCalculationService.calculateTaxes(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(com.example.project.customer.dto.tax.TaxCalculationResult.builder()
                        .subtotal(new BigDecimal("1000"))
                        .grandTotal(new BigDecimal("1000"))
                        .itemResults(cartItems.stream().map(ci -> com.example.project.customer.dto.tax.TaxableItemResult.builder().productId(ci.getProduct().getProductId()).unitPrice(BigDecimal.ZERO).build()).toList())
                        .build());

        cartService.getCart(555);

        // Verification: Exactly 1 single batched query to hinchStoreVendorInventoryRepository, regardless of 20 items!
        verify(hinchStoreVendorInventoryRepository, times(1)).getAvailableStockSummaryForProducts(any());
        verify(hinchStoreVendorInventoryRepository, never()).getAvailableStockForProduct(any());
    }
}
