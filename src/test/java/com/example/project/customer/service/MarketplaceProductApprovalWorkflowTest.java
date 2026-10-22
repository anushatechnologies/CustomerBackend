package com.example.project.customer.service;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.dto.*;
import com.example.project.customer.entity.*;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketplaceProductApprovalWorkflowTest {

    @Mock private ProductRepository productRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private SellerRepository sellerRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private ProductTaxonomyService productTaxonomyService;
    @Mock private HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CartService mockCartService;
    @Mock private CheckoutService mockCheckoutService;
    @Mock private CouponService couponService;
    @Mock private TaxCalculationService taxCalculationService;
    @Mock private StoreInvoiceSequenceService storeInvoiceSequenceService;
    @Mock private SellerPayoutLedgerRepository sellerPayoutLedgerRepository;
    @Mock private CategoryRepository categoryRepository;

    private PlatformMarketplaceProperties platformMarketplaceProperties;
    private SellerProductServiceImpl sellerProductService;
    private ProductServiceImpl productService;
    private ProductAvailabilityServiceImpl availabilityService;
    private CartServiceImpl cartService;
    private OrderServiceImpl orderService;
    private StoreServiceImpl storeService;

    private Seller sellerUnverified;
    private Seller sellerVerified;
    private Store vendorStore;
    private Store platformStore;
    private Category category;
    private Subcategory subcategory;
    private Brand brand;

    @BeforeEach
    void setUp() {
        platformMarketplaceProperties = new PlatformMarketplaceProperties();
        platformMarketplaceProperties.setPlatformStoreId(1);
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        availabilityService = new ProductAvailabilityServiceImpl(
                productRepository, hinchStoreVendorInventoryRepository, platformMarketplaceProperties
        );

        sellerProductService = new SellerProductServiceImpl(
                productRepository, brandRepository, sellerRepository, storeRepository, productTaxonomyService
        );

        productService = new ProductServiceImpl(
                productRepository, brandRepository, null, categoryRepository, storeRepository, null, productTaxonomyService
        );
        productService.setProductAvailabilityService(availabilityService);

        cartService = new CartServiceImpl(
                cartRepository, cartItemRepository, productRepository, storeRepository, couponService, taxCalculationService
        );
        cartService.setProductAvailabilityService(availabilityService);

        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                productRepository,
                addressRepository,
                customerRepository,
                mockCartService,
                mockCheckoutService,
                null,
                storeInvoiceSequenceService,
                sellerPayoutLedgerRepository,
                storeRepository,
                null,
                null
        );
        orderService.setPlatformMarketplaceProperties(platformMarketplaceProperties);
        orderService.setHinchStoreVendorInventoryRepository(hinchStoreVendorInventoryRepository);
        orderService.setProductAvailabilityService(availabilityService);

        storeService = new StoreServiceImpl(
                storeRepository, sellerRepository, productRepository, productService
        );
        storeService.setProductAvailabilityService(availabilityService);

        sellerUnverified = Seller.builder()
                .sellerId(101)
                .name("Unverified Seller")
                .companyName("Unverified Inc")
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        sellerVerified = Seller.builder()
                .sellerId(102)
                .name("Verified Seller")
                .companyName("Verified Corp")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        vendorStore = Store.builder()
                .storeId(10)
                .name("Vendor Store 10")
                .seller(sellerVerified)
                .status(StoreStatus.ACTIVE)
                .build();

        platformStore = Store.builder()
                .storeId(1)
                .name("HinchStore Central")
                .status(StoreStatus.ACTIVE)
                .build();

        category = Category.builder().categoryId(1).name("Electrical").build();
        subcategory = Subcategory.builder().subcategoryId(11).name("Cables").category(category).build();
        brand = Brand.builder().brandId(21).name("Finolex").build();

        when(sellerRepository.findById(101)).thenReturn(Optional.of(sellerUnverified));
        when(sellerRepository.findById(102)).thenReturn(Optional.of(sellerVerified));
        when(storeRepository.findBySeller_SellerId(102)).thenReturn(Optional.of(vendorStore));
        when(storeRepository.findBySellerSellerId(102)).thenReturn(Optional.of(vendorStore));
        when(storeRepository.findById(10)).thenReturn(Optional.of(vendorStore));
        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(brandRepository.findById(21)).thenReturn(Optional.of(brand));
    }

    // 1. Product creation defaults to PENDING, active=false, rejectionReason=null
    @Test
    @DisplayName("1. Seller creating ordinary marketplace product defaults to PENDING approval and inactive")
    void testSellerProductCreationDefaultsToPendingAndInactive() {
        SellerProductCreateRequest request = new SellerProductCreateRequest();
        request.setTitle("Industrial Copper Cable 100m");
        request.setPrice(new BigDecimal("1200.00"));
        request.setStockQty(50);
        request.setBrandId(21);
        request.setSubcategoryId(11);

        when(productTaxonomyService.resolveSubcategoryForProduct(eq(11), any(), any(), eq(brand)))
                .thenReturn(subcategory);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setProductId(501);
            return p;
        });

        ProductResponse response = sellerProductService.createSellerProduct(102, request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();

        assertThat(saved.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(saved.getActive()).isFalse();
        assertThat(saved.getRejectionReason()).isNull();
        assertThat(saved.getStockQty()).isEqualTo(50);
        assertThat(saved.getStore().getStoreId()).isEqualTo(10);

        // Verify hinch_store_vendor_inventory was NEVER touched
        verifyNoInteractions(hinchStoreVendorInventoryRepository);
    }

    // 2. Admin cannot approve product of an unverified seller
    @Test
    @DisplayName("2. Admin cannot approve product if seller is unverified")
    void testAdminCannotApproveUnverifiedSellerProduct() {
        Product pendingProduct = Product.builder()
                .productId(502)
                .title("Unverified Product")
                .store(vendorStore)
                .seller(sellerUnverified)
                .approvalStatus(ApprovalStatus.PENDING)
                .active(false)
                .stockQty(20)
                .build();

        when(productRepository.findById(502)).thenReturn(Optional.of(pendingProduct));

        assertThatThrownBy(() -> productService.approve(502))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not yet verified and approved by admin");

        assertThat(pendingProduct.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(pendingProduct.getActive()).isFalse();
    }

    // 3. Admin can approve verified seller product -> active=true, status=APPROVED
    @Test
    @DisplayName("3. Admin approves product -> sets status APPROVED, active=true, clears rejectionReason")
    void testAdminApprovesProductSuccessfully() {
        Product pendingProduct = Product.builder()
                .productId(503)
                .title("Verified Product")
                .store(vendorStore)
                .seller(sellerVerified)
                .approvalStatus(ApprovalStatus.PENDING)
                .active(false)
                .rejectionReason("Previous note")
                .stockQty(30)
                .build();

        when(productRepository.findById(503)).thenReturn(Optional.of(pendingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.approve(503);

        assertThat(pendingProduct.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(pendingProduct.getActive()).isTrue();
        assertThat(pendingProduct.getRejectionReason()).isNull();
        assertThat(response.getStatus()).isEqualTo(ApprovalStatus.APPROVED.name());
    }

    // 4. Admin can reject product with reason -> active=false, status=REJECTED, stores reason
    @Test
    @DisplayName("4. Admin rejects product -> sets status REJECTED, active=false, stores rejectionReason")
    void testAdminRejectsProductWithReason() {
        Product pendingProduct = Product.builder()
                .productId(504)
                .title("Non-compliant Product")
                .store(vendorStore)
                .seller(sellerVerified)
                .approvalStatus(ApprovalStatus.PENDING)
                .active(false)
                .stockQty(10)
                .build();

        when(productRepository.findById(504)).thenReturn(Optional.of(pendingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRejectionRequest rejectionRequest = new ProductRejectionRequest("Image resolution does not meet guidelines.");
        ProductResponse response = productService.reject(504, rejectionRequest);

        assertThat(pendingProduct.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(pendingProduct.getActive()).isFalse();
        assertThat(pendingProduct.getRejectionReason()).isEqualTo("Image resolution does not meet guidelines.");
        assertThat(response.getStatus()).isEqualTo(ApprovalStatus.REJECTED.name());
        assertThat(response.getRejectionReason()).isEqualTo("Image resolution does not meet guidelines.");
    }

    // 5. Seller update on rejected product resets status to PENDING, clears reason, keeps inactive
    @Test
    @DisplayName("5. Seller resubmitting rejected product resets status to PENDING and clears rejectionReason")
    void testSellerResubmissionResetsStatusToPending() {
        Product rejectedProduct = Product.builder()
                .productId(505)
                .title("Previously Rejected Cable")
                .store(vendorStore)
                .seller(sellerVerified)
                .approvalStatus(ApprovalStatus.REJECTED)
                .active(false)
                .rejectionReason("Image resolution too low")
                .stockQty(15)
                .build();

        when(productRepository.findById(505)).thenReturn(Optional.of(rejectedProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerProductUpdateRequest updateRequest = new SellerProductUpdateRequest();
        updateRequest.setTitle("Updated Cable with High-Res Image");

        sellerProductService.updateSellerProduct(102, 505, updateRequest);

        assertThat(rejectedProduct.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(rejectedProduct.getRejectionReason()).isNull();
        assertThat(rejectedProduct.getActive()).isFalse();
        assertThat(rejectedProduct.getTitle()).isEqualTo("Updated Cable with High-Res Image");
    }

    // 6. ProductAvailabilityServiceImpl blocks unapproved ordinary products
    @Test
    @DisplayName("6. ProductAvailability returns inStock=false with appropriate reason for unapproved products")
    void testProductAvailabilityEnforcesApproval() {
        // A. PENDING product with stock
        Product pendingProduct = Product.builder()
                .productId(601)
                .store(vendorStore)
                .stockQty(100)
                .active(false)
                .approvalStatus(ApprovalStatus.PENDING)
                .build();
        ProductAvailability pendingAvail = availabilityService.getAvailability(pendingProduct);
        assertThat(pendingAvail.isInStock()).isFalse();
        assertThat(pendingAvail.getAvailableQty()).isEqualTo(0);
        assertThat(pendingAvail.getReason()).isEqualTo("PENDING_APPROVAL");

        // B. REJECTED product with stock
        Product rejectedProduct = Product.builder()
                .productId(602)
                .store(vendorStore)
                .stockQty(50)
                .active(false)
                .approvalStatus(ApprovalStatus.REJECTED)
                .build();
        ProductAvailability rejectedAvail = availabilityService.getAvailability(rejectedProduct);
        assertThat(rejectedAvail.isInStock()).isFalse();
        assertThat(rejectedAvail.getAvailableQty()).isEqualTo(0);
        assertThat(rejectedAvail.getReason()).isEqualTo("PRODUCT_REJECTED");

        // C. APPROVED active product with stock -> inStock=true, stock_qty
        Product approvedInStock = Product.builder()
                .productId(603)
                .store(vendorStore)
                .stockQty(40)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();
        ProductAvailability approvedAvail = availabilityService.getAvailability(approvedInStock);
        assertThat(approvedAvail.isInStock()).isTrue();
        assertThat(approvedAvail.getAvailableQty()).isEqualTo(40);
        assertThat(approvedAvail.getReason()).isNull();

        // D. APPROVED active product with zero stock -> inStock=false, OUT_OF_STOCK
        Product approvedOos = Product.builder()
                .productId(604)
                .store(vendorStore)
                .stockQty(0)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();
        ProductAvailability oosAvail = availabilityService.getAvailability(approvedOos);
        assertThat(oosAvail.isInStock()).isFalse();
        assertThat(oosAvail.getAvailableQty()).isEqualTo(0);
        assertThat(oosAvail.getReason()).isEqualTo("OUT_OF_STOCK");

        // E. APPROVED inactive product -> inStock=false, PRODUCT_INACTIVE
        Product approvedInactive = Product.builder()
                .productId(605)
                .store(vendorStore)
                .stockQty(10)
                .active(false)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();
        ProductAvailability inactiveAvail = availabilityService.getAvailability(approvedInactive);
        assertThat(inactiveAvail.isInStock()).isFalse();
        assertThat(inactiveAvail.getAvailableQty()).isEqualTo(10);
        assertThat(inactiveAvail.getReason()).isEqualTo("PRODUCT_INACTIVE");
    }

    // 7. Customer getById denies access to unapproved products
    @Test
    @DisplayName("7. Customer getById denies access to PENDING or REJECTED products")
    void testCustomerGetByIdDeniesUnapprovedProducts() {
        Product pendingProduct = Product.builder()
                .productId(701)
                .title("Pending Product")
                .store(vendorStore)
                .active(false)
                .approvalStatus(ApprovalStatus.PENDING)
                .build();

        Product rejectedProduct = Product.builder()
                .productId(702)
                .title("Rejected Product")
                .store(vendorStore)
                .active(false)
                .approvalStatus(ApprovalStatus.REJECTED)
                .build();

        when(productRepository.findById(701)).thenReturn(Optional.of(pendingProduct));
        when(productRepository.findById(702)).thenReturn(Optional.of(rejectedProduct));

        assertThatThrownBy(() -> productService.getById(701))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> productService.getById(702))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // 8. Store endpoint filters only approved products
    @Test
    @DisplayName("8. Store products endpoint filters only approved and active products")
    void testStoreProductsFiltersApprovedOnly() {
        when(storeRepository.findById(10)).thenReturn(Optional.of(vendorStore));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ApiResponse<List<ProductResponse>> response = storeService.getStoreProducts("10", null, null, null, 1, 20, "newest");

        assertThat(response).isNotNull();
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // 9. CartService blocks unapproved products from being added
    @Test
    @DisplayName("9. CartService blocks unapproved products from being added to cart")
    void testCartServiceBlocksUnapprovedProducts() {
        Product pendingProduct = Product.builder()
                .productId(801)
                .title("Pending Cart Product")
                .store(vendorStore)
                .price(new BigDecimal("50.00"))
                .active(false)
                .approvalStatus(ApprovalStatus.PENDING)
                .stockQty(10)
                .build();

        when(productRepository.findById(801)).thenReturn(Optional.of(pendingProduct));

        CartItemRequest request = new CartItemRequest();
        request.setProductId(801);
        request.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(1, request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("pending admin approval");
    }

    // 10. OrderService blocks unapproved products from checkout
    @Test
    @DisplayName("10. OrderService blocks checkout if cart contains an unapproved product")
    void testOrderServiceBlocksUnapprovedProducts() {
        Product unapprovedProduct = Product.builder()
                .productId(901)
                .title("Unapproved Product")
                .store(vendorStore)
                .price(new BigDecimal("100.00"))
                .active(false)
                .approvalStatus(ApprovalStatus.PENDING)
                .stockQty(10)
                .build();

        CartItemResponse unapprovedItem = CartItemResponse.builder()
                .cartItemId(1)
                .productId(901)
                .title("Unapproved Product")
                .quantity(2)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        CartResponse cartResponse = CartResponse.builder()
                .cartId(1)
                .storeId(10)
                .storeName("Vendor Store 10")
                .items(List.of(unapprovedItem))
                .build();

        when(mockCartService.getCart(999)).thenReturn(cartResponse);
        when(productRepository.findById(901)).thenReturn(Optional.of(unapprovedProduct));

        OrderCreateRequest request = new OrderCreateRequest();
        request.setAddressId(10);
        request.setPaymentMethod("COD");

        assertThatThrownBy(() -> orderService.createOrder(999, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not approved by admin");
    }

    // 11. HinchStore isolation: HinchStore products follow hinch_store_vendor_inventory, not stock_qty
    @Test
    @DisplayName("11. HinchStore isolation: platform store products use vendor inventory and checkout properties")
    void testHinchStoreIsolationFromOrdinaryMarketplaceStock() {
        Product hinchProduct = Product.builder()
                .productId(1001)
                .title("HinchStore Cement")
                .store(platformStore)
                .stockQty(9999) // catalog stock qty should be ignored
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        // When hinch_store_vendor_inventory returns available qty = 75
        List<Object[]> rows = Collections.singletonList(new Object[]{1001, 75, 2L});
        when(hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(List.of(1001)))
                .thenReturn(rows);

        ProductAvailability avail = availabilityService.getAvailability(hinchProduct);

        // Positive eligible vendor inventory -> inStock=true, availableQty=75
        assertThat(avail.isInStock()).isTrue();
        assertThat(avail.getAvailableQty()).isEqualTo(75);
        // Ordinary stock_qty 9999 was ignored
        assertThat(avail.getAvailableQty()).isNotEqualTo(9999);
    }
}
