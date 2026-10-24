package com.example.project.customer.service;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.dto.ProductAvailability;
import com.example.project.customer.dto.SellerProductCreateRequest;
import com.example.project.customer.dto.SellerProductUpdateRequest;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.HinchStoreVendorInventory;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.repository.BrandRepository;
import com.example.project.customer.repository.HinchStoreVendorInventoryRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventorySynchronizationServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;
    @Mock private SellerRepository sellerRepository;
    @Mock private BrandRepository brandRepository;

    private PlatformMarketplaceProperties platformMarketplaceProperties;
    private InventorySynchronizationServiceImpl inventorySyncService;
    private SellerProductServiceImpl sellerProductService;
    private ProductAvailabilityServiceImpl productAvailabilityService;

    private Seller platformSeller;
    private Store platformStore;

    private Seller independentSeller2;
    private Store independentStore2;

    private Seller vendorSeller3;
    private Store vendorStore3;

    private Brand testBrand;

    @BeforeEach
    void setUp() {
        platformMarketplaceProperties = new PlatformMarketplaceProperties();
        platformMarketplaceProperties.setPlatformStoreId(1);
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);

        inventorySyncService = new InventorySynchronizationServiceImpl(
                productRepository, storeRepository, hinchStoreVendorInventoryRepository
        );
        inventorySyncService.setPlatformMarketplaceProperties(platformMarketplaceProperties);

        sellerProductService = new SellerProductServiceImpl(
                productRepository, brandRepository, sellerRepository, storeRepository, null
        );
        sellerProductService.setInventorySynchronizationService(inventorySyncService);

        productAvailabilityService = new ProductAvailabilityServiceImpl(
                productRepository, hinchStoreVendorInventoryRepository, platformMarketplaceProperties
        );

        platformSeller = Seller.builder()
                .sellerId(1).name("Platform Admin").verificationStatus(VerificationStatus.VERIFIED).isDeleted(false).build();
        platformStore = Store.builder()
                .storeId(1).seller(platformSeller).name("HinchStore Central").status(StoreStatus.ACTIVE).build();

        independentSeller2 = Seller.builder()
                .sellerId(2).name("Independent Seller 2").verificationStatus(VerificationStatus.VERIFIED).isDeleted(false).build();
        independentStore2 = Store.builder()
                .storeId(2).seller(independentSeller2).name("Store 2 Electricals").status(StoreStatus.ACTIVE).build();

        vendorSeller3 = Seller.builder()
                .sellerId(3).name("Consignment Vendor 3").verificationStatus(VerificationStatus.VERIFIED).isDeleted(false).build();
        vendorStore3 = Store.builder()
                .storeId(3).seller(vendorSeller3).name("Vendor 3 Wholesale Yard").status(StoreStatus.ACTIVE).build();

        testBrand = Brand.builder().brandId(10).name("CabONE").build();

        when(storeRepository.findById(1)).thenReturn(Optional.of(platformStore));
        when(storeRepository.findById(2)).thenReturn(Optional.of(independentStore2));
        when(storeRepository.findById(3)).thenReturn(Optional.of(vendorStore3));

        when(storeRepository.findBySellerSellerId(1)).thenReturn(Optional.of(platformStore));
        when(storeRepository.findBySellerSellerId(2)).thenReturn(Optional.of(independentStore2));
        when(storeRepository.findBySellerSellerId(3)).thenReturn(Optional.of(vendorStore3));

        when(sellerRepository.findById(1)).thenReturn(Optional.of(platformSeller));
        when(sellerRepository.findById(2)).thenReturn(Optional.of(independentSeller2));
        when(sellerRepository.findById(3)).thenReturn(Optional.of(vendorSeller3));

        when(brandRepository.findById(10)).thenReturn(Optional.of(testBrand));

        when(productRepository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            if (p.getProductId() == null) {
                p.setProductId(1001);
            }
            return p;
        });

        when(hinchStoreVendorInventoryRepository.save(any(HinchStoreVendorInventory.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("1. Independent seller product creation: Stock maintained strictly in products.stock_qty; vendor inventory untouched")
    void testCreateProduct_IndependentSeller_IsolatedToProductStockQty() {
        SellerProductCreateRequest request = SellerProductCreateRequest.builder()
                .title("Independent Copper Wire 90m")
                .brandId(10)
                .price(new BigDecimal("1500.00"))
                .stockQty(100)
                .build();

        var response = sellerProductService.createSellerProduct(2, request);
        assertThat(response).isNotNull();
        assertThat(response.getStockQty()).isEqualTo(100);

        // Verify product was saved with stock 100
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, atLeastOnce()).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getStockQty()).isEqualTo(100);
        assertThat(savedProduct.getStore().getStoreId()).isEqualTo(2);

        // Independent store must NEVER touch HinchStore consignment inventory
        verify(hinchStoreVendorInventoryRepository, never()).save(any(HinchStoreVendorInventory.class));

        // Availability check reads products.stock_qty directly for independent seller
        savedProduct.setApprovalStatus(ApprovalStatus.APPROVED);
        savedProduct.setActive(true);
        ProductAvailability avail = productAvailabilityService.getAvailability(savedProduct);
        assertThat(avail.getAvailableQty()).isEqualTo(100);
        assertThat(avail.isInStock()).isTrue();
        assertThat(avail.getReason()).isNull();
    }

    @Test
    @DisplayName("2. Platform seller product creation: Automatically creates HinchStore vendor inventory record with exact stock")
    void testCreateProduct_PlatformSeller_SyncsVendorInventory() {
        when(hinchStoreVendorInventoryRepository.findByProduct_ProductIdAndVendorStore_StoreId(anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        SellerProductCreateRequest request = SellerProductCreateRequest.builder()
                .title("Something 12W Downlight")
                .brandId(10)
                .price(new BigDecimal("9.00"))
                .stockQty(100)
                .build();

        var response = sellerProductService.createSellerProduct(1, request);
        assertThat(response).isNotNull();

        // Verify HinchStoreVendorInventory was created for Store 1 with qty 100, reserved 0
        ArgumentCaptor<HinchStoreVendorInventory> invCaptor = ArgumentCaptor.forClass(HinchStoreVendorInventory.class);
        verify(hinchStoreVendorInventoryRepository).save(invCaptor.capture());
        HinchStoreVendorInventory savedInv = invCaptor.getValue();

        assertThat(savedInv.getQty()).isEqualTo(100);
        assertThat(savedInv.getReservedQty()).isEqualTo(0);
        assertThat(savedInv.getVendorStore().getStoreId()).isEqualTo(1);
        assertThat(savedInv.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("3. Consignment vendor updates HinchStore product stock: Synchronizes inventory record and preserves isolation")
    void testUpdateStock_PlatformProduct_UpdatesVendorInventory() {
        Product platformProduct = Product.builder()
                .productId(501)
                .store(platformStore)
                .seller(vendorSeller3)
                .title("Wholesale Cement 50kg")
                .stockQty(100)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();
        when(productRepository.findById(501)).thenReturn(Optional.of(platformProduct));

        HinchStoreVendorInventory existingInv = HinchStoreVendorInventory.builder()
                .id(10)
                .product(platformProduct)
                .vendorStore(vendorStore3)
                .qty(100)
                .reservedQty(0)
                .isActive(true)
                .build();
        when(hinchStoreVendorInventoryRepository.findByProduct_ProductIdAndVendorStore_StoreId(501, 3))
                .thenReturn(Optional.of(existingInv));

        // Vendor updates stock to 250
        sellerProductService.updateSellerStock(3, 501, 250);

        assertThat(existingInv.getQty()).isEqualTo(250);
        assertThat(platformProduct.getStockQty()).isEqualTo(250);
        verify(hinchStoreVendorInventoryRepository).save(existingInv);
    }

    @Test
    @DisplayName("4. Repeated stock updates: Updates existing row idempotently without duplicate row creation")
    void testRepeatedStockUpdates_NoDuplicateRows() {
        Product platformProduct = Product.builder()
                .productId(601)
                .store(platformStore)
                .seller(platformSeller)
                .stockQty(50)
                .build();
        when(productRepository.findById(601)).thenReturn(Optional.of(platformProduct));

        HinchStoreVendorInventory existingInv = HinchStoreVendorInventory.builder()
                .id(25)
                .product(platformProduct)
                .vendorStore(platformStore)
                .qty(50)
                .reservedQty(0)
                .isActive(true)
                .build();
        when(hinchStoreVendorInventoryRepository.findByProduct_ProductIdAndVendorStore_StoreId(601, 1))
                .thenReturn(Optional.of(existingInv));

        // Multiple consecutive updates
        inventorySyncService.syncProductInventory(platformProduct, 75, 1);
        inventorySyncService.syncProductInventory(platformProduct, 120, 1);
        inventorySyncService.syncProductInventory(platformProduct, 180, 1);

        assertThat(existingInv.getQty()).isEqualTo(180);
        // Ensure the single existing entity was saved, no new entities constructed
        verify(hinchStoreVendorInventoryRepository, times(3)).save(existingInv);
    }

    @Test
    @DisplayName("5. Stock update strictly preserves active reserved quantities from pending customer orders")
    void testStockUpdate_PreservesReservedStock() {
        Product platformProduct = Product.builder()
                .productId(701)
                .store(platformStore)
                .seller(platformSeller)
                .stockQty(100)
                .build();

        // Vendor has 100 stock with 35 currently reserved for an in-flight order
        HinchStoreVendorInventory invWithReservations = HinchStoreVendorInventory.builder()
                .id(99)
                .product(platformProduct)
                .vendorStore(platformStore)
                .qty(100)
                .reservedQty(35)
                .isActive(true)
                .build();
        when(hinchStoreVendorInventoryRepository.findByProduct_ProductIdAndVendorStore_StoreId(701, 1))
                .thenReturn(Optional.of(invWithReservations));

        // Seller inward shipment arrives, updating stock to 160
        inventorySyncService.syncProductInventory(platformProduct, 160, 1);

        assertThat(invWithReservations.getQty()).isEqualTo(160);
        // Reserved quantity must remain untouched at 35
        assertThat(invWithReservations.getReservedQty()).isEqualTo(35);
        // Available qty is 160 - 35 = 125
        assertThat(invWithReservations.getAvailableQty()).isEqualTo(125);
    }

    @Test
    @DisplayName("6. Independent seller stock updates remain completely segregated from HinchStore")
    void testIndependentSellerStockUpdate_Segregated() {
        Product indProduct = Product.builder()
                .productId(801)
                .store(independentStore2)
                .seller(independentSeller2)
                .stockQty(40)
                .build();
        when(productRepository.findById(801)).thenReturn(Optional.of(indProduct));

        sellerProductService.updateSellerStock(2, 801, 90);

        assertThat(indProduct.getStockQty()).isEqualTo(90);
        verify(productRepository, atLeastOnce()).save(indProduct);
        verify(hinchStoreVendorInventoryRepository, never()).save(any(HinchStoreVendorInventory.class));
    }

    @Test
    @DisplayName("7. Startup reconciliation backfills missing HinchStore inventory while ignoring independent products")
    void testReconciliation_MissingRecordsBackfilled() {
        Product missing1 = Product.builder().productId(901).title("Product 1").store(platformStore).seller(platformSeller).stockQty(80).build();
        Product missing2 = Product.builder().productId(902).title("Product 2").store(platformStore).seller(vendorSeller3).stockQty(45).build();
        Product zeroStock = Product.builder().productId(903).title("Product 3").store(platformStore).seller(platformSeller).stockQty(0).build();

        when(productRepository.findByStore_StoreId(1)).thenReturn(List.of(missing1, missing2, zeroStock));
        when(hinchStoreVendorInventoryRepository.findByProduct_ProductIdAndVendorStore_StoreId(anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        inventorySyncService.reconcileExistingPlatformInventories();

        // Exactly 2 missing products with positive stock backfilled
        ArgumentCaptor<HinchStoreVendorInventory> captor = ArgumentCaptor.forClass(HinchStoreVendorInventory.class);
        verify(hinchStoreVendorInventoryRepository, times(2)).save(captor.capture());

        List<HinchStoreVendorInventory> savedInvs = captor.getAllValues();
        assertThat(savedInvs).hasSize(2);

        HinchStoreVendorInventory inv1 = savedInvs.get(0);
        assertThat(inv1.getProduct().getProductId()).isEqualTo(901);
        assertThat(inv1.getVendorStore().getStoreId()).isEqualTo(1);
        assertThat(inv1.getQty()).isEqualTo(80);
        assertThat(inv1.getReservedQty()).isEqualTo(0);

        HinchStoreVendorInventory inv2 = savedInvs.get(1);
        assertThat(inv2.getProduct().getProductId()).isEqualTo(902);
        assertThat(inv2.getVendorStore().getStoreId()).isEqualTo(3);
        assertThat(inv2.getQty()).isEqualTo(45);
        assertThat(inv2.getReservedQty()).isEqualTo(0);
    }

    @Test
    @DisplayName("8. Availability service reports inStock=true and correct available count immediately after sync")
    void testAvailabilityReportsInStockAfterSync() {
        Product platformProduct = Product.builder()
                .productId(1136)
                .store(platformStore)
                .seller(platformSeller)
                .title("CabONE Something")
                .stockQty(100)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();
        when(productRepository.findById(1136)).thenReturn(Optional.of(platformProduct));

        // Before sync, 0 rows in vendor inventory -> UNAVAILABLE_NO_VENDOR_STOCK
        when(hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(List.of(1136)))
                .thenReturn(Collections.emptyList());
        ProductAvailability beforeAvail = productAvailabilityService.getAvailability(platformProduct);
        assertThat(beforeAvail.isInStock()).isFalse();
        assertThat(beforeAvail.getAvailableQty()).isEqualTo(0);
        assertThat(beforeAvail.getReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");

        // Sync occurs (e.g. seller added with 100 stock)
        when(hinchStoreVendorInventoryRepository.findByProduct_ProductIdAndVendorStore_StoreId(1136, 1))
                .thenReturn(Optional.empty());
        inventorySyncService.syncProductInventory(platformProduct, 100, 1);

        // After sync, repository returns the newly available stock
        List<Object[]> summary = Collections.singletonList(new Object[]{1136, 100, 1L});
        when(hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(List.of(1136)))
                .thenReturn(summary);

        ProductAvailability afterAvail = productAvailabilityService.getAvailability(platformProduct);
        assertThat(afterAvail.isInStock()).isTrue();
        assertThat(afterAvail.getAvailableQty()).isEqualTo(100);
        assertThat(afterAvail.getReason()).isNull();
    }

    @Test
    @DisplayName("9. StoreStatus enum correctly parses INACTIVE database status without throwing IllegalArgumentException")
    void testStoreStatus_ParsesInactive() {
        StoreStatus status = StoreStatus.valueOf("INACTIVE");
        assertThat(status).isEqualTo(StoreStatus.INACTIVE);
        assertThat(status).isNotEqualTo(StoreStatus.ACTIVE);
    }

    @Test
    @DisplayName("10. Inactive stores are not treated as active inventory suppliers during reconciliation")
    void testReconciliation_WithInactiveStore_DoesNotTreatAsInventorySupplier() {
        Seller inactiveSeller = Seller.builder().sellerId(4).name("Inactive Seller").build();
        Store inactiveStore = Store.builder()
                .storeId(4)
                .seller(inactiveSeller)
                .name("Inactive Store 4")
                .status(StoreStatus.INACTIVE)
                .build();

        Product prodWithInactiveStore = Product.builder()
                .productId(904)
                .title("Product from Inactive Store")
                .store(platformStore)
                .seller(inactiveSeller)
                .stockQty(50)
                .build();

        when(storeRepository.findBySellerSellerId(4)).thenReturn(Optional.of(inactiveStore));
        when(productRepository.findByStore_StoreId(1)).thenReturn(List.of(prodWithInactiveStore));

        // Must complete without throwing IllegalArgumentException: No enum constant StoreStatus.INACTIVE
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                inventorySyncService.reconcileExistingPlatformInventories()
        );

        // Verify inactive store is NOT treated as an active inventory supplier
        verify(hinchStoreVendorInventoryRepository, never()).save(any(HinchStoreVendorInventory.class));
    }

    @Test
    @DisplayName("11. Reconciliation continues processing eligible stores when an inactive store is encountered")
    void testReconciliation_ContinuesProcessingEligibleStoresWhenInactiveStoreEncountered() {
        Seller inactiveSeller = Seller.builder().sellerId(4).name("Inactive Seller").build();
        Store inactiveStore = Store.builder()
                .storeId(4)
                .seller(inactiveSeller)
                .name("Inactive Store 4")
                .status(StoreStatus.INACTIVE)
                .build();

        Seller activeSeller = Seller.builder().sellerId(5).name("Active Seller").build();
        Store activeStore = Store.builder()
                .storeId(5)
                .seller(activeSeller)
                .name("Active Store 5")
                .status(StoreStatus.ACTIVE)
                .build();

        Product prodInactive = Product.builder()
                .productId(904)
                .title("Product from Inactive Store")
                .store(platformStore)
                .seller(inactiveSeller)
                .stockQty(50)
                .build();

        Product prodActive = Product.builder()
                .productId(905)
                .title("Product from Active Store")
                .store(platformStore)
                .seller(activeSeller)
                .stockQty(80)
                .build();

        when(storeRepository.findBySellerSellerId(4)).thenReturn(Optional.of(inactiveStore));
        when(storeRepository.findBySellerSellerId(5)).thenReturn(Optional.of(activeStore));
        when(productRepository.findByStore_StoreId(1)).thenReturn(List.of(prodInactive, prodActive));
        when(hinchStoreVendorInventoryRepository.findByProduct_ProductIdAndVendorStore_StoreId(905, 5))
                .thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                inventorySyncService.reconcileExistingPlatformInventories()
        );

        // Verify inactive store product is NOT saved
        verify(hinchStoreVendorInventoryRepository, never()).save(argThat(inv ->
                inv.getProduct().getProductId().equals(904) || inv.getVendorStore().getStoreId().equals(4)));

        // Verify active store product IS saved
        verify(hinchStoreVendorInventoryRepository).save(argThat(inv ->
                inv.getProduct().getProductId().equals(905) &&
                inv.getVendorStore().getStoreId().equals(5) &&
                inv.getQty() == 80 &&
                Boolean.TRUE.equals(inv.getIsActive())));
    }
}
