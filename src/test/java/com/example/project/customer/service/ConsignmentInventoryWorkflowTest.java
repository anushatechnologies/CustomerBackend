package com.example.project.customer.service;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.ProductAvailability;
import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.StockQuantityUpdateRequest;
import com.example.project.customer.dto.consignment.HinchStoreInventoryRequest;
import com.example.project.customer.dto.consignment.HinchStoreInventoryResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Cart;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.HinchStoreVendorInventory;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderInventoryState;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.entity.PayoutLedgerStatus;
import com.example.project.customer.entity.SellerPayoutLedger;
import org.mockito.ArgumentCaptor;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.BrandRepository;
import com.example.project.customer.repository.CartItemRepository;
import com.example.project.customer.repository.CartRepository;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.HinchStoreVendorInventoryRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsignmentInventoryWorkflowTest {

    @Mock private ProductRepository productRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private HinchStoreVendorInventoryRepository inventoryRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SellerPayoutLedgerRepository sellerPayoutLedgerRepository;
    @Mock private CartService cartService;
    @Mock private CheckoutService checkoutService;
    @Mock private StoreInvoiceSequenceService storeInvoiceSequenceService;
    @Mock private CouponService couponService;
    @Mock private TaxCalculationService taxCalculationService;
    @Mock private BrandRepository brandRepository;
    @Mock private SubcategoryRepository subcategoryRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private S3ImageService s3ImageService;
    @Mock private ProductTaxonomyService productTaxonomyService;

    private PlatformMarketplaceProperties platformMarketplaceProperties;
    private VendorSelectionServiceImpl vendorSelectionService;
    private HinchStoreInventoryServiceImpl hinchStoreInventoryService;
    private ProductAvailabilityServiceImpl availabilityService;
    private CartServiceImpl cartServiceImpl;
    private CheckoutServiceImpl checkoutServiceImpl;
    private OrderServiceImpl orderService;
    private ProductServiceImpl productServiceImpl;

    private Store platformStore;
    private Store eligibleVendorStore1;
    private Store eligibleVendorStore2;
    private Store inactiveStore;
    private Store unverifiedSellerStore;
    private Store deletedSellerStore;

    private Seller platformSeller;
    private Seller verifiedSeller1;
    private Seller verifiedSeller2;
    private Seller unverifiedSeller;
    private Seller deletedSeller;

    private Product platformProduct1;
    private Product platformProduct2;

    private Customer testCustomer;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        platformMarketplaceProperties = new PlatformMarketplaceProperties();
        // Safe defaults
        platformMarketplaceProperties.setPlatformStoreId(1);
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        vendorSelectionService = new VendorSelectionServiceImpl(inventoryRepository, platformMarketplaceProperties);
        hinchStoreInventoryService = new HinchStoreInventoryServiceImpl(
                inventoryRepository, productRepository, storeRepository, platformMarketplaceProperties
        );
        availabilityService = new ProductAvailabilityServiceImpl(
                productRepository, inventoryRepository, platformMarketplaceProperties
        );

        cartServiceImpl = new CartServiceImpl(
                cartRepository, cartItemRepository, productRepository, storeRepository, couponService, taxCalculationService
        );
        cartServiceImpl.setProductAvailabilityService(availabilityService);

        productServiceImpl = new ProductServiceImpl(
                productRepository,
                brandRepository,
                subcategoryRepository,
                categoryRepository,
                storeRepository,
                s3ImageService,
                productTaxonomyService
        );
        productServiceImpl.setHinchStoreVendorInventoryRepository(inventoryRepository);
        productServiceImpl.setProductAvailabilityService(availabilityService);
        productServiceImpl.setPlatformMarketplaceProperties(platformMarketplaceProperties);

        checkoutServiceImpl = new CheckoutServiceImpl(
                cartServiceImpl, addressRepository, storeRepository, taxCalculationService
        );

        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                productRepository,
                addressRepository,
                customerRepository,
                cartService,
                checkoutService,
                null,
                storeInvoiceSequenceService,
                sellerPayoutLedgerRepository,
                storeRepository,
                null,
                null
        );
        orderService.setPlatformMarketplaceProperties(platformMarketplaceProperties);
        orderService.setVendorSelectionService(vendorSelectionService);
        orderService.setHinchStoreVendorInventoryRepository(inventoryRepository);
        orderService.setProductAvailabilityService(availabilityService);

        // Setup entities
        platformSeller = Seller.builder().sellerId(1).name("HinchMart Admin")
                .verificationStatus(VerificationStatus.VERIFIED).isDeleted(false).build();
        platformStore = Store.builder().storeId(1).seller(platformSeller).name("HinchStore Central")
                .status(StoreStatus.ACTIVE).latitude(17.40).longitude(78.40).build();

        verifiedSeller1 = Seller.builder().sellerId(101).name("Verified Vendor 1")
                .verificationStatus(VerificationStatus.VERIFIED).isDeleted(false).build();
        eligibleVendorStore1 = Store.builder().storeId(10).seller(verifiedSeller1).name("Vendor Store 10")
                .status(StoreStatus.ACTIVE).latitude(17.44).longitude(78.38).build();

        verifiedSeller2 = Seller.builder().sellerId(102).name("Verified Vendor 2")
                .verificationStatus(VerificationStatus.VERIFIED).isDeleted(false).build();
        eligibleVendorStore2 = Store.builder().storeId(20).seller(verifiedSeller2).name("Vendor Store 20")
                .status(StoreStatus.ACTIVE).latitude(17.46).longitude(78.39).build();

        inactiveStore = Store.builder().storeId(30).seller(verifiedSeller1).name("Closed Store 30")
                .status(StoreStatus.CLOSED).latitude(17.45).longitude(78.38).build();

        unverifiedSeller = Seller.builder().sellerId(103).name("Pending KYC Seller")
                .verificationStatus(VerificationStatus.PENDING).isDeleted(false).build();
        unverifiedSellerStore = Store.builder().storeId(40).seller(unverifiedSeller).name("Unverified Store 40")
                .status(StoreStatus.ACTIVE).latitude(17.45).longitude(78.38).build();

        deletedSeller = Seller.builder().sellerId(104).name("Deleted Seller")
                .verificationStatus(VerificationStatus.VERIFIED).isDeleted(true).build();
        deletedSellerStore = Store.builder().storeId(50).seller(deletedSeller).name("Deleted Seller Store 50")
                .status(StoreStatus.ACTIVE).latitude(17.45).longitude(78.38).build();

        platformProduct1 = Product.builder().productId(201).store(platformStore).title("Cement 50kg")
                .stockQty(100).price(new BigDecimal("350.00")).active(true)
                .approvalStatus(ApprovalStatus.APPROVED).build();

        platformProduct2 = Product.builder().productId(202).store(platformStore).title("Steel Rod 12mm")
                .stockQty(100).price(new BigDecimal("500.00")).active(true)
                .approvalStatus(ApprovalStatus.APPROVED).build();

        testCustomer = Customer.builder().customerId(999).name("Test Builder").phone("9876543210").build();
        testAddress = Address.builder().id(88).customer(testCustomer).addressLine1("Site 12, Madhapur")
                .latitude(17.45).longitude(78.38).build();

        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        when(productRepository.findById(202)).thenReturn(Optional.of(platformProduct2));
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(storeRepository.findById(10)).thenReturn(Optional.of(eligibleVendorStore1));
        when(storeRepository.findById(20)).thenReturn(Optional.of(eligibleVendorStore2));
        when(storeRepository.findById(30)).thenReturn(Optional.of(inactiveStore));
        when(storeRepository.findById(40)).thenReturn(Optional.of(unverifiedSellerStore));
        when(storeRepository.findById(50)).thenReturn(Optional.of(deletedSellerStore));

        when(taxCalculationService.calculateTaxes(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(com.example.project.customer.dto.tax.TaxCalculationResult.builder()
                        .subtotal(BigDecimal.ZERO)
                        .discount(BigDecimal.ZERO)
                        .taxableAmount(BigDecimal.ZERO)
                        .cgst(BigDecimal.ZERO)
                        .sgst(BigDecimal.ZERO)
                        .igst(BigDecimal.ZERO)
                        .totalGst(BigDecimal.ZERO)
                        .freightCharge(BigDecimal.ZERO)
                        .craneUnloadingCharge(BigDecimal.ZERO)
                        .grandTotal(BigDecimal.ZERO)
                        .itemResults(Collections.emptyList())
                        .build());
        when(taxCalculationService.isIntraState(any(), any())).thenReturn(true);
    }

    // 1. Checkout disabled by default
    @Test
    @DisplayName("1. Checkout feature flag defaults to disabled; blocks order creation, availability, and cart add")
    void testCheckoutDisabledByDefault() {
        // Assert property default
        PlatformMarketplaceProperties freshProperties = new PlatformMarketplaceProperties();
        assertThat(freshProperties.isHinchStoreCheckoutEnabled()).isFalse();

        // When checkout is disabled, order creation throws IllegalStateException
        CartItemResponse ci = CartItemResponse.builder()
                .productId(201).title("Cement 50kg").quantity(2)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("700.00")).build();
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();
        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));

        assertThatThrownBy(() -> orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HinchStore checkout is temporarily disabled pending Phase 2 vendor consignment rollout.");

        // Product availability returns CHECKOUT_DISABLED with availableQty of physical stock
        List<Object[]> summaryRows = Collections.singletonList(new Object[]{201, 50, 1L});
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(201)))
                .thenReturn(summaryRows);

        ProductAvailability avail = availabilityService.getAvailability(platformProduct1);
        assertThat(avail.getAvailableQty()).isEqualTo(50);
        assertThat(avail.isInStock()).isTrue();
        assertThat(avail.getReason()).isEqualTo("CHECKOUT_DISABLED");

        // CartService rejects adding item with CHECKOUT_DISABLED
        CartItemRequest req = CartItemRequest.builder().productId(201).quantity(1).build();
        assertThatThrownBy(() -> cartServiceImpl.addItem(999, req))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("CHECKOUT_DISABLED");
    }

    // 2. No eligible vendor inventory means unavailable
    @Test
    @DisplayName("2. No eligible vendor inventory means unavailable (UNAVAILABLE_NO_VENDOR_STOCK)")
    void testNoEligibleVendorInventoryMeansUnavailable() {
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        // No rows in repository for product
        List<Object[]> emptyRows = Collections.emptyList();
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(201)))
                .thenReturn(emptyRows);

        // Availability returns UNAVAILABLE_NO_VENDOR_STOCK
        ProductAvailability avail = availabilityService.getAvailability(platformProduct1);
        assertThat(avail.getAvailableQty()).isEqualTo(0);
        assertThat(avail.isInStock()).isFalse();
        assertThat(avail.getReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");

        // CartService rejects adding with UNAVAILABLE_NO_VENDOR_STOCK
        CartItemRequest req = CartItemRequest.builder().productId(201).quantity(1).build();
        assertThatThrownBy(() -> cartServiceImpl.addItem(999, req))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("UNAVAILABLE_NO_VENDOR_STOCK");

        // Checkout preview blocks order
        Cart emptyOrUnavailableCart = Cart.builder().cartId(1).store(platformStore).build();
        when(cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(999)).thenReturn(Optional.of(emptyOrUnavailableCart));
        when(addressRepository.findByCustomer_CustomerIdAndId(999, 88)).thenReturn(Optional.of(testAddress));

        CheckoutPreviewResponse preview = checkoutServiceImpl.previewCheckout(999, CheckoutPreviewRequest.builder().addressId(88).build());
        assertThat(preview.getCanPlaceOrder()).isFalse();
        assertThat(preview.getOrderBlockReason()).isNotNull();
    }

    // 3a. HinchStore (store_id = 1) is eligible when valid and can fulfill orders
    @Test
    @DisplayName("3a. HinchStore (store_id = 1) is eligible when valid and can fulfill orders with its inventory")
    void testHinchStoreEligibleAsFulfillmentVendorWhenValid() {
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        // Verify isEligibleVendor returns true for active platformStore with verified non-deleted seller
        assertThat(vendorSelectionService.isEligibleVendor(platformStore)).isTrue();

        HinchStoreVendorInventory platformStoreInv = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(platformStore).qty(100).reservedQty(0).isActive(true).build();

        when(inventoryRepository.findByProduct_ProductIdInAndIsActiveTrue(any()))
                .thenReturn(List.of(platformStoreInv));

        CartItemResponse item = CartItemResponse.builder()
                .productId(201).title("Cement 50kg").quantity(5)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("1750.00")).build();
        List<Store> candidates = vendorSelectionService.getRankedCandidateVendors(List.of(item), testAddress);

        // HinchStore is selected as eligible candidate
        assertThat(candidates).contains(platformStore);

        // Order creation using HinchStore fulfillment succeeds
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(item)).build();
        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        when(addressRepository.findByCustomer_CustomerIdAndId(999, 88)).thenReturn(Optional.of(testAddress));
        when(customerRepository.findById(999)).thenReturn(Optional.of(testCustomer));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("1750.00")).grandTotal(new BigDecimal("1750.00")).build();
        when(checkoutService.previewCheckout(eq(999), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(platformStore)).thenReturn("INV-HINCH-001");

        when(inventoryRepository.atomicReserve(eq(201), eq(1), eq(5))).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(9001);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse resp = orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build());
        assertThat(resp.getStoreId()).isEqualTo(1);
        assertThat(resp.getFulfillmentStoreId()).isEqualTo(1);
        assertThat(resp.getInventoryState()).isEqualTo("RESERVED");

        verify(inventoryRepository).atomicReserve(201, 1, 5);
    }

    // 3b. HinchStore is unavailable when stock is insufficient
    @Test
    @DisplayName("3b. HinchStore is unavailable when stock is insufficient")
    void testHinchStoreUnavailableWhenStockIsInsufficient() {
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        // Only 2 available in inventory, but cart requires 5
        HinchStoreVendorInventory lowStockInv = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(platformStore).qty(2).reservedQty(0).isActive(true).build();

        when(inventoryRepository.findByProduct_ProductIdInAndIsActiveTrue(any()))
                .thenReturn(List.of(lowStockInv));

        CartItemResponse requesting5 = CartItemResponse.builder()
                .productId(201).title("Cement 50kg").quantity(5)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("1750.00")).build();

        List<Store> candidates = vendorSelectionService.getRankedCandidateVendors(List.of(requesting5), testAddress);
        assertThat(candidates).isEmpty();

        // Order creation fails because HinchStore cannot fulfill the order
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(requesting5)).build();
        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        when(addressRepository.findByCustomer_CustomerIdAndId(999, 88)).thenReturn(Optional.of(testAddress));
        when(customerRepository.findById(999)).thenReturn(Optional.of(testCustomer));

        assertThatThrownBy(() -> orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("No consignment vendor has sufficient stock to fulfill this order.");
    }

    // 4. Inactive or unverified vendors are excluded
    @Test
    @DisplayName("4. Inactive, unverified, or soft-deleted vendors are excluded from candidate evaluation and mapping")
    void testInactiveOrUnverifiedVendorsAreExcluded() {
        HinchStoreVendorInventory invInactive = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(inactiveStore).qty(100).reservedQty(0).isActive(true).build();
        HinchStoreVendorInventory invUnverified = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(unverifiedSellerStore).qty(100).reservedQty(0).isActive(true).build();
        HinchStoreVendorInventory invDeleted = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(deletedSellerStore).qty(100).reservedQty(0).isActive(true).build();

        when(inventoryRepository.findByProduct_ProductIdInAndIsActiveTrue(any()))
                .thenReturn(List.of(invInactive, invUnverified, invDeleted));

        CartItemResponse item = CartItemResponse.builder().productId(201).quantity(2).build();
        List<Store> candidates = vendorSelectionService.getRankedCandidateVendors(List.of(item), testAddress);

        assertThat(candidates).isEmpty();
        assertThat(vendorSelectionService.isEligibleVendor(inactiveStore)).isFalse();
        assertThat(vendorSelectionService.isEligibleVendor(unverifiedSellerStore)).isFalse();
        assertThat(vendorSelectionService.isEligibleVendor(deletedSellerStore)).isFalse();

        // In HinchStoreInventoryServiceImpl, mapping to inactive/unverified/deleted is rejected
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        when(storeRepository.findById(30)).thenReturn(Optional.of(inactiveStore));
        when(storeRepository.findById(40)).thenReturn(Optional.of(unverifiedSellerStore));
        when(storeRepository.findById(50)).thenReturn(Optional.of(deletedSellerStore));

        HinchStoreInventoryRequest reqInactive = HinchStoreInventoryRequest.builder().productId(201).vendorStoreId(30).qty(50).build();
        assertThatThrownBy(() -> hinchStoreInventoryService.mapOrUpdateInventory(reqInactive))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor store must be active");

        HinchStoreInventoryRequest reqUnverified = HinchStoreInventoryRequest.builder().productId(201).vendorStoreId(40).qty(50).build();
        assertThatThrownBy(() -> hinchStoreInventoryService.mapOrUpdateInventory(reqUnverified))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor seller must be verified");

        HinchStoreInventoryRequest reqDeleted = HinchStoreInventoryRequest.builder().productId(201).vendorStoreId(50).qty(50).build();
        assertThatThrownBy(() -> hinchStoreInventoryService.mapOrUpdateInventory(reqDeleted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor seller is soft-deleted");
    }

    // 5. A single eligible vendor can fulfill the entire cart
    @Test
    @DisplayName("5. A single eligible vendor fulfills the entire cart with atomic reservation and state RESERVED")
    void testSingleEligibleVendorFulfillsEntireCart() {
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        CartItemResponse item1 = CartItemResponse.builder()
                .productId(201).title("Cement 50kg").quantity(3)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("1050.00")).build();
        CartItemResponse item2 = CartItemResponse.builder()
                .productId(202).title("Steel Rod 12mm").quantity(2)
                .unitPrice(new BigDecimal("500.00")).lineTotal(new BigDecimal("1000.00")).build();

        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(item1, item2)).build();
        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        when(productRepository.findById(202)).thenReturn(Optional.of(platformProduct2));
        when(addressRepository.findByCustomer_CustomerIdAndId(999, 88)).thenReturn(Optional.of(testAddress));
        when(customerRepository.findById(999)).thenReturn(Optional.of(testCustomer));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("2050.00")).grandTotal(new BigDecimal("2050.00")).build();
        when(checkoutService.previewCheckout(eq(999), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(platformStore)).thenReturn("INV-101");

        // Vendor 10 has stock for both items:
        HinchStoreVendorInventory v10Item1 = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(eligibleVendorStore1).qty(20).reservedQty(0).isActive(true).build();
        HinchStoreVendorInventory v10Item2 = HinchStoreVendorInventory.builder()
                .product(platformProduct2).vendorStore(eligibleVendorStore1).qty(20).reservedQty(0).isActive(true).build();
        when(inventoryRepository.findByProduct_ProductIdInAndIsActiveTrue(any()))
                .thenReturn(List.of(v10Item1, v10Item2));

        // Atomic reservation succeeds for both items at Store 10
        when(inventoryRepository.atomicReserve(eq(201), eq(10), eq(3))).thenReturn(1);
        when(inventoryRepository.atomicReserve(eq(202), eq(10), eq(2))).thenReturn(1);

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(8001);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse resp = orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build());

        assertThat(resp.getStoreId()).isEqualTo(1);
        assertThat(resp.getFulfillmentStoreId()).isEqualTo(10);
        assertThat(resp.getFulfillmentStoreName()).isEqualTo("Vendor Store 10");
        assertThat(resp.getInventoryState()).isEqualTo("RESERVED");

        verify(inventoryRepository).atomicReserve(201, 10, 3);
        verify(inventoryRepository).atomicReserve(202, 10, 2);
    }

    // 6. A cart requiring multiple vendors is rejected under no-split-order policy
    @Test
    @DisplayName("6. A cart requiring multiple vendors is rejected under no-split-order policy")
    void testCartRequiringMultipleVendorsIsRejected() {
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        CartItemResponse item1 = CartItemResponse.builder()
                .productId(201).title("Cement 50kg").quantity(5)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("1750.00")).build();
        CartItemResponse item2 = CartItemResponse.builder()
                .productId(202).title("Steel Rod 12mm").quantity(5)
                .unitPrice(new BigDecimal("500.00")).lineTotal(new BigDecimal("2500.00")).build();

        // Vendor 10 has Cement (item1) but NOT Steel (item2)
        HinchStoreVendorInventory v10Item1 = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(eligibleVendorStore1).qty(20).reservedQty(0).isActive(true).build();

        // Vendor 20 has Steel (item2) but NOT Cement (item1)
        HinchStoreVendorInventory v20Item2 = HinchStoreVendorInventory.builder()
                .product(platformProduct2).vendorStore(eligibleVendorStore2).qty(20).reservedQty(0).isActive(true).build();

        when(inventoryRepository.findByProduct_ProductIdInAndIsActiveTrue(any()))
                .thenReturn(List.of(v10Item1, v20Item2));

        // Candidate evaluation: neither vendor can satisfy both items -> candidates list must be empty
        List<Store> candidates = vendorSelectionService.getRankedCandidateVendors(List.of(item1, item2), testAddress);
        assertThat(candidates).isEmpty();

        // Order creation fails with ResourceConflictException
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(item1, item2)).build();
        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        when(productRepository.findById(202)).thenReturn(Optional.of(platformProduct2));
        when(addressRepository.findByCustomer_CustomerIdAndId(999, 88)).thenReturn(Optional.of(testAddress));
        when(customerRepository.findById(999)).thenReturn(Optional.of(testCustomer));

        assertThatThrownBy(() -> orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("No consignment vendor has sufficient stock to fulfill this order.");
    }

    // 7. Insufficient stock and reservation behavior
    @Test
    @DisplayName("7. Insufficient stock and reservation behavior: candidate failover and rollback")
    void testInsufficientStockAndReservationBehavior() {
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        // Case 7a: Candidate store has insufficient stock
        HinchStoreVendorInventory v10ShortStock = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(eligibleVendorStore1).qty(4).reservedQty(0).isActive(true).build();
        when(inventoryRepository.findByProduct_ProductIdInAndIsActiveTrue(any()))
                .thenReturn(List.of(v10ShortStock));

        CartItemResponse requesting5 = CartItemResponse.builder().productId(201).quantity(5).build();
        List<Store> candidates = vendorSelectionService.getRankedCandidateVendors(List.of(requesting5), testAddress);
        assertThat(candidates).isEmpty();

        // Case 7b: Race condition during atomic reservation -> rolls back and tries next candidate
        CartItemResponse ci = CartItemResponse.builder()
                .productId(201).title("Cement 50kg").quantity(2)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("700.00")).build();
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();
        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        when(addressRepository.findByCustomer_CustomerIdAndId(999, 88)).thenReturn(Optional.of(testAddress));
        when(customerRepository.findById(999)).thenReturn(Optional.of(testCustomer));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("700.00")).grandTotal(new BigDecimal("700.00")).build();
        when(checkoutService.previewCheckout(eq(999), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(platformStore)).thenReturn("INV-102");

        // Both stores 10 and 20 are candidates
        HinchStoreVendorInventory inv10 = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(eligibleVendorStore1).qty(10).reservedQty(0).isActive(true).build();
        HinchStoreVendorInventory inv20 = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(eligibleVendorStore2).qty(10).reservedQty(0).isActive(true).build();
        when(inventoryRepository.findByProduct_ProductIdInAndIsActiveTrue(any()))
                .thenReturn(List.of(inv10, inv20));

        // Store 10 fails reservation (0 updated), Store 20 succeeds (1 updated)
        when(inventoryRepository.atomicReserve(eq(201), eq(10), eq(2))).thenReturn(0);
        when(inventoryRepository.atomicReserve(eq(201), eq(20), eq(2))).thenReturn(1);

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(8002);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build());
        assertThat(response.getFulfillmentStoreId()).isEqualTo(20);

        // Case 7c: If all candidates fail reservation, throws ResourceConflictException
        when(inventoryRepository.atomicReserve(eq(201), eq(20), eq(2))).thenReturn(0);
        assertThatThrownBy(() -> orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Consignment stock reservation failed across all candidate vendor stores.");
    }

    // 8. Fix 1: Seller payout attribution regression test
    @Test
    @DisplayName("8. Consignment order credits the fulfillment vendor seller, not the platform seller")
    void testConsignmentSellerPayoutAttribution_PlatformAndVendorSellerDifferent() {
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        // Platform seller is sellerId=1, vendor seller is sellerId=101. Both non-null and different.
        assertThat(platformStore.getSeller()).isNotNull();
        assertThat(platformStore.getSeller().getSellerId()).isEqualTo(1);
        assertThat(eligibleVendorStore1.getSeller()).isNotNull();
        assertThat(eligibleVendorStore1.getSeller().getSellerId()).isEqualTo(101);
        assertThat(platformStore.getSeller().getSellerId()).isNotEqualTo(eligibleVendorStore1.getSeller().getSellerId());

        CartItemResponse ci = CartItemResponse.builder()
                .productId(201).title("Cement 50kg").quantity(2)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("700.00")).build();
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();

        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        when(addressRepository.findByCustomer_CustomerIdAndId(999, 88)).thenReturn(Optional.of(testAddress));
        when(customerRepository.findById(999)).thenReturn(Optional.of(testCustomer));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("700.00")).grandTotal(new BigDecimal("700.00")).build();
        when(checkoutService.previewCheckout(eq(999), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(platformStore)).thenReturn("INV-HINCH-888");

        HinchStoreVendorInventory v10Inv = HinchStoreVendorInventory.builder()
                .product(platformProduct1).vendorStore(eligibleVendorStore1).qty(20).reservedQty(0).isActive(true).build();
        when(inventoryRepository.findByProduct_ProductIdInAndIsActiveTrue(any()))
                .thenReturn(List.of(v10Inv));
        when(inventoryRepository.atomicReserve(eq(201), eq(10), eq(2))).thenReturn(1);

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(9901);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse resp = orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build());

        assertThat(resp.getStoreId()).isEqualTo(1);
        assertThat(resp.getFulfillmentStoreId()).isEqualTo(10);
        assertThat(resp.getInventoryState()).isEqualTo("RESERVED");

        // Verify OrderItem references the fulfillment vendor seller (101)
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, atLeastOnce()).save(itemCaptor.capture());
        OrderItem savedItem = itemCaptor.getValue();
        assertThat(savedItem.getSeller()).isNotNull();
        assertThat(savedItem.getSeller().getSellerId()).isEqualTo(101);
        assertThat(savedItem.getSeller().getSellerId()).isNotEqualTo(platformStore.getSeller().getSellerId());
        assertThat(savedItem.getFulfillmentStore().getStoreId()).isEqualTo(10);
        assertThat(savedItem.getIsHinchStore()).isFalse();

        // Verify SellerPayoutLedger credits the fulfillment vendor seller (101), NOT the platform store seller (1)
        ArgumentCaptor<SellerPayoutLedger> ledgerCaptor = ArgumentCaptor.forClass(SellerPayoutLedger.class);
        verify(sellerPayoutLedgerRepository).save(ledgerCaptor.capture());
        SellerPayoutLedger savedLedger = ledgerCaptor.getValue();
        assertThat(savedLedger.getSeller()).isNotNull();
        assertThat(savedLedger.getSeller().getSellerId()).isEqualTo(101);
        assertThat(savedLedger.getSeller().getSellerId()).isNotEqualTo(1);
        assertThat(savedLedger.getStore().getStoreId()).isEqualTo(10);
        assertThat(savedLedger.getStatus()).isEqualTo(PayoutLedgerStatus.PENDING);
        assertThat(savedLedger.getGrossAmount()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(savedLedger.getTcsAmount()).isEqualByComparingTo(BigDecimal.ZERO); // TCS exempt for HinchStore
    }

    // 9. Fix 2a: Feature flag disabled -> fails closed immediately before reservation or side effects
    @Test
    @DisplayName("9. HinchStore order fails closed when checkout is disabled; no side effects occur")
    void testHinchStoreOrder_FailsClosedWhenCheckoutDisabled_NoSideEffects() {
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        CartItemResponse ci = CartItemResponse.builder()
                .productId(201).title("Cement 50kg").quantity(2)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("700.00")).build();
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();

        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));

        assertThatThrownBy(() -> orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("HinchStore checkout is temporarily disabled pending Phase 2 vendor consignment rollout.");

        // Assert zero side effects
        verify(inventoryRepository, never()).atomicReserve(anyInt(), anyInt(), anyInt());
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
        verify(sellerPayoutLedgerRepository, never()).save(any());
        verify(cartService, never()).clearCart(anyInt());
    }

    // 10. Fix 2b: Properties null -> fails closed immediately before reservation or side effects
    @Test
    @DisplayName("10. HinchStore order fails closed when marketplace properties are null; no side effects occur")
    void testHinchStoreOrder_FailsClosedWhenPropertiesNull_NoSideEffects() {
        orderService.setPlatformMarketplaceProperties(null);

        CartItemResponse ci = CartItemResponse.builder()
                .productId(201).title("Cement 50kg").quantity(2)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("700.00")).build();
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();

        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));

        assertThatThrownBy(() -> orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("HinchStore checkout is temporarily disabled pending Phase 2 vendor consignment rollout.");

        // Assert zero side effects
        verify(inventoryRepository, never()).atomicReserve(anyInt(), anyInt(), anyInt());
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
        verify(sellerPayoutLedgerRepository, never()).save(any());
        verify(cartService, never()).clearCart(anyInt());
    }

    // 11. Ordinary marketplace orders: existing behavior preserved
    @Test
    @DisplayName("11. Ordinary marketplace orders preserve standard stock decrement and credit vendor store seller")
    void testOrdinaryMarketplaceOrder_PreservesExistingBehaviorAndCreditsVendorStoreSeller() {
        Product vendorProduct = Product.builder()
                .productId(301).store(eligibleVendorStore1).seller(verifiedSeller1)
                .title("Vendor Tiles").stockQty(50).price(new BigDecimal("120.00")).active(true)
                .approvalStatus(ApprovalStatus.APPROVED).build();

        CartItemResponse ci = CartItemResponse.builder()
                .productId(301).title("Vendor Tiles").quantity(2)
                .unitPrice(new BigDecimal("120.00")).lineTotal(new BigDecimal("240.00")).build();
        CartResponse cart = CartResponse.builder().storeId(10).items(List.of(ci)).build();

        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(10)).thenReturn(Optional.of(eligibleVendorStore1));
        when(productRepository.findById(301)).thenReturn(Optional.of(vendorProduct));
        when(productRepository.findByIdForStockUpdate(301)).thenReturn(Optional.of(vendorProduct));
        when(addressRepository.findByCustomer_CustomerIdAndId(999, 88)).thenReturn(Optional.of(testAddress));
        when(customerRepository.findById(999)).thenReturn(Optional.of(testCustomer));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(new BigDecimal("240.00")).grandTotal(new BigDecimal("240.00")).build();
        when(checkoutService.previewCheckout(eq(999), any())).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(eligibleVendorStore1)).thenReturn("INV-V10-001");

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            if (o.getOrderId() == null) o.setOrderId(7001);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse resp = orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build());

        assertThat(resp.getStoreId()).isEqualTo(10);
        assertThat(resp.getFulfillmentStoreId()).isEqualTo(10);
        assertThat(resp.getInventoryState()).isEqualTo("NONE");

        // Consignment reservation was never invoked
        verify(inventoryRepository, never()).atomicReserve(anyInt(), anyInt(), anyInt());

        // Regular stock decrement was executed
        assertThat(vendorProduct.getStockQty()).isEqualTo(48);

        // Seller payout ledger credits the vendor store seller with TCS deducted
        ArgumentCaptor<SellerPayoutLedger> ledgerCaptor = ArgumentCaptor.forClass(SellerPayoutLedger.class);
        verify(sellerPayoutLedgerRepository).save(ledgerCaptor.capture());
        SellerPayoutLedger savedLedger = ledgerCaptor.getValue();
        assertThat(savedLedger.getSeller()).isNotNull();
        assertThat(savedLedger.getSeller().getSellerId()).isEqualTo(101);
        assertThat(savedLedger.getStore().getStoreId()).isEqualTo(10);
        assertThat(savedLedger.getStatus()).isEqualTo(PayoutLedgerStatus.PENDING);
        assertThat(savedLedger.getGrossAmount()).isEqualByComparingTo(new BigDecimal("240.00"));
        assertThat(savedLedger.getTcsAmount()).isEqualByComparingTo(new BigDecimal("2.40"));
        assertThat(savedLedger.getNetPayoutAmount()).isEqualByComparingTo(new BigDecimal("237.60"));
    }

    // 12. Stock availability vs checkout eligibility separation
    @Test
    @DisplayName("12. Availability separation: positive stock is inStock=true with CHECKOUT_DISABLED; zero stock is OUT_OF_STOCK; missing is UNAVAILABLE_NO_VENDOR_STOCK; inactive is PRODUCT_INACTIVE")
    void testHinchStoreAvailabilitySeparationFromCheckoutEligibility() {
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        // A. Positive eligible inventory -> inStock=true, reason=CHECKOUT_DISABLED
        Product productPositive = Product.builder().productId(501).store(platformStore).title("Positive Stock Product").active(true).build();
        List<Object[]> positiveRows = Collections.singletonList(new Object[]{501, 25, 1L});
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(501)))
                .thenReturn(positiveRows);
        ProductAvailability availPositive = availabilityService.getAvailability(productPositive);
        assertThat(availPositive.getAvailableQty()).isEqualTo(25);
        assertThat(availPositive.isInStock()).isTrue();
        assertThat(availPositive.getReason()).isEqualTo("CHECKOUT_DISABLED");

        // When flag is enabled, positive stock has reason=null
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);
        ProductAvailability availPositiveFlagOn = availabilityService.getAvailability(productPositive);
        assertThat(availPositiveFlagOn.getAvailableQty()).isEqualTo(25);
        assertThat(availPositiveFlagOn.isInStock()).isTrue();
        assertThat(availPositiveFlagOn.getReason()).isNull();

        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        // B. Zero available quantity with vendor rows -> inStock=false, reason=OUT_OF_STOCK
        Product productZero = Product.builder().productId(502).store(platformStore).title("Zero Stock Product").active(true).build();
        List<Object[]> zeroRows = Collections.singletonList(new Object[]{502, 0, 1L});
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(502)))
                .thenReturn(zeroRows);
        ProductAvailability availZero = availabilityService.getAvailability(productZero);
        assertThat(availZero.getAvailableQty()).isEqualTo(0);
        assertThat(availZero.isInStock()).isFalse();
        assertThat(availZero.getReason()).isEqualTo("OUT_OF_STOCK");

        // C. Missing / ineligible inventory (no rows) -> inStock=false, reason=UNAVAILABLE_NO_VENDOR_STOCK
        Product productMissing = Product.builder().productId(503).store(platformStore).title("Missing Inventory Product").active(true).build();
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(503)))
                .thenReturn(Collections.emptyList());
        ProductAvailability availMissing = availabilityService.getAvailability(productMissing);
        assertThat(availMissing.getAvailableQty()).isEqualTo(0);
        assertThat(availMissing.isInStock()).isFalse();
        assertThat(availMissing.getReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");

        // D. Inactive product -> inStock=false, reason=PRODUCT_INACTIVE
        Product productInactive = Product.builder().productId(504).store(platformStore).title("Inactive Product").active(false).build();
        List<Object[]> inactiveRows = Collections.singletonList(new Object[]{504, 50, 1L});
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(504)))
                .thenReturn(inactiveRows);
        ProductAvailability availInactive = availabilityService.getAvailability(productInactive);
        assertThat(availInactive.getAvailableQty()).isEqualTo(50);
        assertThat(availInactive.isInStock()).isFalse();
        assertThat(availInactive.getReason()).isEqualTo("PRODUCT_INACTIVE");

        // E. Ordinary marketplace product -> untouched, based on product's own stock, active state, and approval status
        Product vendorProduct = Product.builder().productId(505).store(eligibleVendorStore1).title("Vendor Product").stockQty(15).active(true).approvalStatus(ApprovalStatus.APPROVED).build();
        ProductAvailability availVendor = availabilityService.getAvailability(vendorProduct);
        assertThat(availVendor.getAvailableQty()).isEqualTo(15);
        assertThat(availVendor.isInStock()).isTrue();
        assertThat(availVendor.getReason()).isNull();

        Product vendorProductOos = Product.builder().productId(506).store(eligibleVendorStore1).title("Vendor OOS").stockQty(0).active(true).approvalStatus(ApprovalStatus.APPROVED).build();
        ProductAvailability availVendorOos = availabilityService.getAvailability(vendorProductOos);
        assertThat(availVendorOos.getAvailableQty()).isEqualTo(0);
        assertThat(availVendorOos.isInStock()).isFalse();
        assertThat(availVendorOos.getReason()).isEqualTo("OUT_OF_STOCK");
    }

    // 13. Store 1 is not accepted as a vendor in this architecture
    @Test
    @DisplayName("13. Store 1 is not accepted as a vendor in HinchStore vendor inventory")
    void testStore1NotAcceptedAsVendor() {
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));

        HinchStoreInventoryRequest request = new HinchStoreInventoryRequest();
        request.setProductId(201);
        request.setVendorStoreId(1);
        request.setQty(250);

        assertThatThrownBy(() -> hinchStoreInventoryService.mapOrUpdateInventory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor store cannot be the platform store.");

        verify(inventoryRepository, never()).save(any());
    }

    // 14. Creating a catalog product does not fabricate a vendor inventory mapping
    @Test
    @DisplayName("14. Creating a catalog product does not fabricate a vendor inventory mapping")
    void testAdminCreateHinchStoreProduct_DoesNotFabricateInventory() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getProductId() == null) {
                p.setProductId(601);
            }
            return p;
        });

        Brand testBrand = Brand.builder().brandId(50).name("HinchBrand").build();
        when(brandRepository.findById(50)).thenReturn(Optional.of(testBrand));

        ProductRequest req = ProductRequest.builder()
                .brandId(50)
                .title("HinchStore Waterproof Cement")
                .description("High grade cement")
                .price(new BigDecimal("450.00"))
                .stockQty(75) // Admin entered 75 in request
                .unit("bag")
                .build();

        // No vendor inventory mapping exists
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(601)))
                .thenReturn(Collections.emptyList());

        ProductResponse createResp = productServiceImpl.create(req);
        assertThat(createResp).isNotNull();
        assertThat(createResp.getProductId()).isEqualTo(601);
        // Available qty is 0 because no vendor inventory mapping was fabricated
        assertThat(createResp.getStockQty()).isEqualTo(0);
        assertThat(createResp.getAvailableQty()).isEqualTo(0);
        assertThat(createResp.getInStock()).isFalse();
        assertThat(createResp.getAvailabilityReason()).isEqualTo("PRODUCT_INACTIVE");

        // Verify inventory mapping was NEVER saved or fabricated
        verify(inventoryRepository, never()).save(any());

        // Even when admin approves product, with no vendor mapping it fails closed as UNAVAILABLE_NO_VENDOR_STOCK
        Product createdProduct = Product.builder()
                .productId(601)
                .store(platformStore)
                .title("HinchStore Waterproof Cement")
                .stockQty(75)
                .price(new BigDecimal("450.00"))
                .active(false)
                .approvalStatus(ApprovalStatus.PENDING)
                .build();
        when(productRepository.findById(601)).thenReturn(Optional.of(createdProduct));

        ProductResponse approveResp = productServiceImpl.approve(601);
        assertThat(approveResp.getInStock()).isFalse();
        assertThat(approveResp.getAvailableQty()).isEqualTo(0);
        assertThat(approveResp.getAvailabilityReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");

        // Customer fetching product
        when(productRepository.findByProductIdAndApprovalStatusAndActive(601, ApprovalStatus.APPROVED, true))
                .thenReturn(Optional.of(createdProduct));

        ProductResponse customerResp = productServiceImpl.getById(601);
        assertThat(customerResp.getInStock()).isFalse();
        assertThat(customerResp.getAvailableQty()).isEqualTo(0);
        assertThat(customerResp.getAvailabilityReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");

        // Confirm inventoryRepository.save was never called throughout
        verify(inventoryRepository, never()).save(any());
    }

    // 15. Assigning explicit stock to an eligible vendor creates or updates the correct mapping and produces inStock=true
    @Test
    @DisplayName("15. Assigning explicit stock to an eligible vendor creates or updates mapping and produces inStock=true")
    void testAssignStockToEligibleVendor_ProducesInStockTrue() {
        when(storeRepository.findById(10)).thenReturn(Optional.of(eligibleVendorStore1));
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        when(inventoryRepository.findByProduct_ProductIdAndVendorStore_StoreId(201, 10))
                .thenReturn(Optional.empty());
        when(inventoryRepository.save(any(HinchStoreVendorInventory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        HinchStoreInventoryRequest request = new HinchStoreInventoryRequest();
        request.setProductId(201);
        request.setVendorStoreId(10);
        request.setQty(50);

        HinchStoreInventoryResponse result = hinchStoreInventoryService.mapOrUpdateInventory(request);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(201);
        assertThat(result.getVendorStoreId()).isEqualTo(10);
        assertThat(result.getQty()).isEqualTo(50);
        assertThat(result.getReservedQty()).isEqualTo(0);
        assertThat(result.getAvailableQty()).isEqualTo(50);
        assertThat(result.getIsActive()).isTrue();

        // Verify product.stockQty is NOT overwritten by vendor mapping
        assertThat(platformProduct1.getStockQty()).isEqualTo(100);
        verify(productRepository, never()).save(platformProduct1);

        // When availability is checked for product 201 with this eligible vendor stock
        when(productRepository.findByProductIdAndApprovalStatusAndActive(201, ApprovalStatus.APPROVED, true))
                .thenReturn(Optional.of(platformProduct1));
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(201)))
                .thenReturn(Collections.singletonList(new Object[]{201, 50, 1L}));

        ProductResponse customerResp = productServiceImpl.getById(201);
        assertThat(customerResp.getAvailableQty()).isEqualTo(50);
        assertThat(customerResp.getInStock()).isTrue();
        assertThat(customerResp.getAvailabilityReason()).isEqualTo("CHECKOUT_DISABLED");
    }

    // 16. Existing vendor quantities and reserved quantities are preserved unless explicitly changed through inventory workflow
    @Test
    @DisplayName("16. Existing vendor quantities and reserved quantities are preserved unless explicitly changed through inventory workflow")
    void testExistingVendorQuantitiesAndReservedQuantitiesPreserved() {
        HinchStoreVendorInventory existingInv = HinchStoreVendorInventory.builder()
                .id(710)
                .product(platformProduct1)
                .vendorStore(eligibleVendorStore1)
                .qty(50)
                .reservedQty(15) // Existing reservation
                .isActive(true)
                .build();

        when(storeRepository.findById(10)).thenReturn(Optional.of(eligibleVendorStore1));
        when(productRepository.findById(201)).thenReturn(Optional.of(platformProduct1));
        when(inventoryRepository.findByProduct_ProductIdAndVendorStore_StoreId(201, 10))
                .thenReturn(Optional.of(existingInv));
        when(inventoryRepository.save(any(HinchStoreVendorInventory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 1. Explicitly update vendor inventory qty to 80
        HinchStoreInventoryRequest updateReq = new HinchStoreInventoryRequest();
        updateReq.setProductId(201);
        updateReq.setVendorStoreId(10);
        updateReq.setQty(80);

        HinchStoreInventoryResponse updateResp = hinchStoreInventoryService.mapOrUpdateInventory(updateReq);
        assertThat(updateResp.getQty()).isEqualTo(80);
        assertThat(updateResp.getReservedQty()).isEqualTo(15); // Preserved!
        assertThat(updateResp.getAvailableQty()).isEqualTo(65); // 80 - 15 = 65

        // Verify product.stockQty is not overwritten
        assertThat(platformProduct1.getStockQty()).isEqualTo(100);
        verify(productRepository, never()).save(platformProduct1);

        // 2. Modifying catalog product details does NOT overwrite or touch vendor inventory
        Brand testBrand = Brand.builder().brandId(50).name("HinchBrand").build();
        when(brandRepository.findById(50)).thenReturn(Optional.of(testBrand));

        ProductRequest prodReq = ProductRequest.builder()
                .brandId(50)
                .title("Cement 50kg Ultra")
                .price(new BigDecimal("380.00"))
                .stockQty(999) // Admin changed stockQty in catalog form
                .unit("bag")
                .build();

        reset(inventoryRepository);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(201)))
                .thenReturn(Collections.singletonList(new Object[]{201, 65, 1L}));

        ProductResponse prodResp = productServiceImpl.update(201, prodReq);
        assertThat(prodResp).isNotNull();
        // Availability remains derived from vendor inventory (65), NOT from the 999 in productRequest
        assertThat(prodResp.getAvailableQty()).isEqualTo(65);
        assertThat(prodResp.getInStock()).isTrue();
        // Verify inventoryRepository was never saved/overwritten during product update
        verify(inventoryRepository, never()).save(any());
        // Vendor inventory reservedQty and qty remain intact
        assertThat(existingInv.getQty()).isEqualTo(80);
        assertThat(existingInv.getReservedQty()).isEqualTo(15);
    }

    // 17. Zero available inventory produces inStock=false
    @Test
    @DisplayName("17. Zero available inventory produces inStock=false")
    void testZeroAvailableInventory_ProducesInStockFalse() {
        when(productRepository.findByProductIdAndApprovalStatusAndActive(201, ApprovalStatus.APPROVED, true))
                .thenReturn(Optional.of(platformProduct1));

        // Case A: Vendor mapping has qty=10, reservedQty=10 -> availableQty=0
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(201)))
                .thenReturn(Collections.singletonList(new Object[]{201, 0, 1L}));

        ProductResponse respA = productServiceImpl.getById(201);
        assertThat(respA.getAvailableQty()).isEqualTo(0);
        assertThat(respA.getInStock()).isFalse();
        assertThat(respA.getAvailabilityReason()).isEqualTo("OUT_OF_STOCK");

        // Case B: Vendor mapping has qty=0, reservedQty=0 -> availableQty=0
        ProductAvailability avail = availabilityService.getAvailability(platformProduct1);
        assertThat(avail.getAvailableQty()).isEqualTo(0);
        assertThat(avail.isInStock()).isFalse();
        assertThat(avail.getReason()).isEqualTo("OUT_OF_STOCK");
    }

    // 18. Missing or ineligible vendor inventory produces inStock=false
    @Test
    @DisplayName("18. Missing or ineligible vendor inventory produces inStock=false")
    void testMissingOrIneligibleVendorInventory_ProducesInStockFalse() {
        when(productRepository.findByProductIdAndApprovalStatusAndActive(201, ApprovalStatus.APPROVED, true))
                .thenReturn(Optional.of(platformProduct1));

        // Case A: Missing mapping (no rows)
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(201)))
                .thenReturn(Collections.emptyList());

        ProductResponse respMissing = productServiceImpl.getById(201);
        assertThat(respMissing.getAvailableQty()).isEqualTo(0);
        assertThat(respMissing.getInStock()).isFalse();
        assertThat(respMissing.getAvailabilityReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");

        // Case B: Ineligible vendor store / seller
        // In the database, the query filters for store.status='ACTIVE' AND seller.verificationStatus='VERIFIED' AND seller.isDeleted=false.
        // Therefore, when inventory only exists at inactiveStore (store 30), unverifiedSellerStore (store 40), or deletedSellerStore (store 50),
        // the available inventory summary returns empty list.
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(201)))
                .thenReturn(Collections.emptyList());

        ProductAvailability availIneligible = availabilityService.getAvailability(platformProduct1);
        assertThat(availIneligible.getAvailableQty()).isEqualTo(0);
        assertThat(availIneligible.isInStock()).isFalse();
        assertThat(availIneligible.getReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");
    }

    // 19. Ordinary marketplace behavior is unchanged
    @Test
    @DisplayName("19. Ordinary marketplace behavior is unchanged")
    void testOrdinaryMarketplaceProduct_PreservesNormalMarketplaceBehavior() {
        // Ordinary marketplace product owned by vendor store (storeId=10)
        Product vendorProd = Product.builder()
                .productId(606)
                .store(eligibleVendorStore1)
                .title("Vendor Bricks")
                .stockQty(40)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        when(productRepository.findByProductIdAndApprovalStatusAndActive(606, ApprovalStatus.APPROVED, true))
                .thenReturn(Optional.of(vendorProd));

        // 1. Fetching vendor product uses its own stockQty directly, ignoring HinchStore inventory
        ProductResponse resp = productServiceImpl.getById(606);
        assertThat(resp.getAvailableQty()).isEqualTo(40);
        assertThat(resp.getInStock()).isTrue();
        assertThat(resp.getAvailabilityReason()).isNull();

        // 2. Updating stock quantity on vendor product updates product.stockQty and does not touch inventoryRepository
        when(productRepository.findByIdForStockUpdate(606)).thenReturn(Optional.of(vendorProd));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        StockQuantityUpdateRequest stockReq = new StockQuantityUpdateRequest();
        stockReq.setStockQty(10);
        ProductResponse updatedResp = productServiceImpl.updateStockQuantity(606, stockReq);
        assertThat(updatedResp.getAvailableQty()).isEqualTo(50);
        assertThat(updatedResp.getInStock()).isTrue();
        assertThat(updatedResp.getAvailabilityReason()).isNull();

        // Verify inventoryRepository was never touched for vendor store product
        verify(inventoryRepository, never()).findByProduct_ProductIdAndVendorStore_StoreId(eq(606), any());

        // 3. When stock is 0, normal marketplace product is out of stock
        Product vendorOosProd = Product.builder()
                .productId(607)
                .store(eligibleVendorStore1)
                .title("Vendor Cement OOS")
                .stockQty(0)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();
        when(productRepository.findByProductIdAndApprovalStatusAndActive(607, ApprovalStatus.APPROVED, true))
                .thenReturn(Optional.of(vendorOosProd));

        ProductResponse oosResp = productServiceImpl.getById(607);
        assertThat(oosResp.getAvailableQty()).isEqualTo(0);
        assertThat(oosResp.getInStock()).isFalse();
        assertThat(oosResp.getAvailabilityReason()).isEqualTo("OUT_OF_STOCK");
    }

    // 20. Checkout remains blocked while disabled
    @Test
    @DisplayName("20. Checkout remains blocked while disabled")
    void testCheckoutRemainsBlockedWhileDisabled() {
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        CartItemResponse ci = CartItemResponse.builder()
                .productId(201).title("Cement 50kg").quantity(2)
                .unitPrice(new BigDecimal("350.00")).lineTotal(new BigDecimal("700.00")).build();
        CartResponse cart = CartResponse.builder().storeId(1).items(List.of(ci)).build();

        when(cartService.getCart(999)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));

        // Order creation fails closed immediately
        assertThatThrownBy(() -> orderService.createOrder(999, OrderCreateRequest.builder().addressId(88).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("HinchStore checkout is temporarily disabled pending Phase 2 vendor consignment rollout.");

        // Product availability reflects checkout disabled
        when(inventoryRepository.getAvailableStockSummaryForProducts(List.of(201)))
                .thenReturn(Collections.singletonList(new Object[]{201, 10, 1L}));
        ProductAvailability avail = availabilityService.getAvailability(platformProduct1);
        assertThat(avail.isInStock()).isTrue();
        assertThat(avail.getReason()).isEqualTo("CHECKOUT_DISABLED");

        // Zero side effects
        verify(inventoryRepository, never()).atomicReserve(anyInt(), anyInt(), anyInt());
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
        verify(sellerPayoutLedgerRepository, never()).save(any());
        verify(cartService, never()).clearCart(anyInt());
    }
}

