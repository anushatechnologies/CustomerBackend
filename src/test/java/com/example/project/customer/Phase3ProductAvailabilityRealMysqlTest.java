package com.example.project.customer;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.ProductAvailability;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Cart;
import com.example.project.customer.entity.CartItem;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.HomeHotDealsResponse;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.HotDeal;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CartItemRepository;
import com.example.project.customer.repository.CartRepository;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.HinchStoreVendorInventoryRepository;
import com.example.project.customer.repository.HotDealRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import com.example.project.customer.service.CartService;
import com.example.project.customer.service.CheckoutService;
import com.example.project.customer.service.HotDealService;
import com.example.project.customer.service.ProductAvailabilityService;
import com.example.project.customer.service.ProductService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIf("isDockerAvailable")
public class Phase3ProductAvailabilityRealMysqlTest {

    static boolean isDockerAvailable() {
        boolean requireDocker = Boolean.getBoolean("require.docker")
                || "true".equalsIgnoreCase(System.getProperty("require.docker"));
        boolean available = false;
        try {
            available = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            available = false;
        }
        if (requireDocker && !available) {
            org.junit.jupiter.api.Assertions.fail(
                    "Docker is required (-Drequire.docker=true) but Docker daemon is not running or available!");
        }
        return available;
    }

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("hinchmart_availability_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (isDockerAvailable()) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
            registry.add("razorpay.key-id", () -> "rzp_test_mockKey");
            registry.add("razorpay.key-secret", () -> "mockSecretKey123");
            registry.add("razorpay.webhook-secret", () -> "mockWebhookSecret123");
        }
    }

    @MockBean
    private com.google.firebase.FirebaseApp firebaseApp;

    @MockBean
    private com.google.firebase.auth.FirebaseAuth firebaseAuth;

    @MockBean
    private com.razorpay.RazorpayClient razorpayClient;

    @Autowired
    private ProductAvailabilityService productAvailabilityService;

    @Autowired
    private HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private PlatformMarketplaceProperties platformMarketplaceProperties;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductService productService;

    @Autowired
    private HotDealService hotDealService;

    @Autowired
    private HotDealRepository hotDealRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubcategoryRepository subcategoryRepository;

    private Store platformStore;
    private Store vendorStore1;
    private Store vendorStore2;
    private Product vendorProduct;
    private Product platformProductWithStock;
    private Product platformProductNoRows;
    private Customer testCustomer;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
        jdbcTemplate.execute("DELETE FROM hot_deals;");
        jdbcTemplate.execute("DELETE FROM cart_items;");
        jdbcTemplate.execute("DELETE FROM carts;");
        jdbcTemplate.execute("DELETE FROM hinch_store_vendor_inventory;");
        jdbcTemplate.execute("DELETE FROM products;");
        jdbcTemplate.execute("DELETE FROM subcategories;");
        jdbcTemplate.execute("DELETE FROM categories;");
        jdbcTemplate.execute("DELETE FROM stores;");
        jdbcTemplate.execute("DELETE FROM sellers;");
        jdbcTemplate.execute("DELETE FROM user_addresses;");
        jdbcTemplate.execute("DELETE FROM customers;");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");

        // 1. Sellers
        Seller seller1 = sellerRepository.save(Seller.builder()
                .name("Vendor One")
                .companyName("Vendor Co 1")
                .email("vendor1@hinchmart.com")
                .phone("+919000000001")
                .verificationStatus(VerificationStatus.VERIFIED)
                .isDeleted(false)
                .build());

        Seller seller2 = sellerRepository.save(Seller.builder()
                .name("Vendor Two")
                .companyName("Vendor Co 2")
                .email("vendor2@hinchmart.com")
                .phone("+919000000002")
                .verificationStatus(VerificationStatus.VERIFIED)
                .isDeleted(false)
                .build());

        Seller platformSeller = sellerRepository.save(Seller.builder()
                .name("Platform Admin Seller")
                .companyName("HinchMart Platform")
                .email("platform@hinchmart.com")
                .phone("+919000000000")
                .verificationStatus(VerificationStatus.VERIFIED)
                .isDeleted(false)
                .build());

        // 2. Stores
        platformStore = storeRepository.save(Store.builder()
                .seller(platformSeller)
                .name("HinchStore Central")
                .slug("hinchstore-central")
                .status(StoreStatus.ACTIVE)
                .build());
        platformMarketplaceProperties.setPlatformStoreId(platformStore.getStoreId());

        vendorStore1 = storeRepository.save(Store.builder()
                .seller(seller1)
                .name("Vendor Store 1")
                .slug("vendor-store-1")
                .status(StoreStatus.ACTIVE)
                .build());

        vendorStore2 = storeRepository.save(Store.builder()
                .seller(seller2)
                .name("Vendor Store 2")
                .slug("vendor-store-2")
                .status(StoreStatus.ACTIVE)
                .build());

        // 3. Products
        vendorProduct = productRepository.save(Product.builder()
                .productId(101)
                .store(vendorStore1)
                .title("Vendor TMT Steel Bar 12mm")
                .slug("vendor-tmt-12mm")
                .price(new BigDecimal("450.00"))
                .unit("pcs")
                .stockQty(15)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build());

        platformProductWithStock = productRepository.save(Product.builder()
                .productId(201)
                .store(platformStore)
                .title("Platform Cement 50kg")
                .slug("platform-cement-50kg")
                .price(new BigDecimal("380.00"))
                .unit("bag")
                .stockQty(999) // Raw catalog stock should be ignored for HinchStore
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build());

        platformProductNoRows = productRepository.save(Product.builder()
                .productId(202)
                .store(platformStore)
                .title("Platform Red Brick 1000pcs")
                .slug("platform-red-brick")
                .price(new BigDecimal("8000.00"))
                .unit("pallet")
                .stockQty(500)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build());

        // 4. Customer & Address
        testCustomer = customerRepository.save(Customer.builder()
                .customerId(99)
                .name("Rishi Testbuyer")
                .email("buyer@hinchmart.com")
                .phone("+919951949353")
                .active(true)
                .role("BUYER")
                .build());

        testAddress = addressRepository.save(Address.builder()
                .id(991)
                .customer(testCustomer)
                .siteName("Office HQ")
                .recipientName("Rishi Testbuyer")
                .phone("+919951949353")
                .addressLine1("Plot 12, HITEC City")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("500081")
                .isDefault(true)
                .build());

        // 5. Vendor inventory rows for platformProductWithStock
        // Vendor 1: qty = 50, reserved_qty = 10 -> available 40
        jdbcTemplate.update("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) VALUES (?, ?, ?, ?, ?)",
                platformProductWithStock.getProductId(), vendorStore1.getStoreId(), 50, 10, true);
        // Vendor 2: qty = 30, reserved_qty = 5  -> available 25
        jdbcTemplate.update("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) VALUES (?, ?, ?, ?, ?)",
                platformProductWithStock.getProductId(), vendorStore2.getStoreId(), 30, 5, true);
    }

    @Test
    @DisplayName("Vendor-own: available == stock_qty exactly")
    void testVendorOwnAvailabilityEqualsStockQty() {
        ProductAvailability avail = productAvailabilityService.getAvailability(vendorProduct);
        assertThat(avail.getAvailableQty()).isEqualTo(15);
        assertThat(avail.isInStock()).isTrue();
        assertThat(avail.getReason()).isNull();
    }

    @Test
    @DisplayName("HinchStore: available == SUM(qty - reserved_qty) across 2 vendors")
    void testHinchStoreSumMinusReservedAcrossTwoVendors() {
        // Direct real repository query
        List<Object[]> summary = hinchStoreVendorInventoryRepository.getAvailableStockSummaryForProducts(List.of(platformProductWithStock.getProductId()));
        assertThat(summary).hasSize(1);
        Object[] row = summary.get(0);
        assertThat(((Number) row[0]).intValue()).isEqualTo(platformProductWithStock.getProductId());
        assertThat(((Number) row[1]).intValue()).isEqualTo(65); // 40 + 25
        assertThat(((Number) row[2]).longValue()).isEqualTo(2L); // 2 vendors

        // Shared availability service with checkout flag enabled
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(true);
        ProductAvailability availEnabled = productAvailabilityService.getAvailability(platformProductWithStock);
        assertThat(availEnabled.getAvailableQty()).isEqualTo(65);
        assertThat(availEnabled.isInStock()).isTrue();
        assertThat(availEnabled.getReason()).isNull();

        // Shared availability service with checkout flag disabled
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);
        ProductAvailability availDisabled = productAvailabilityService.getAvailability(platformProductWithStock);
        assertThat(availDisabled.getAvailableQty()).isEqualTo(65);
        assertThat(availDisabled.isInStock()).isTrue();
        assertThat(availDisabled.getReason()).isEqualTo("CHECKOUT_DISABLED");
    }

    @Test
    @DisplayName("HinchStore with no inventory rows -> UNAVAILABLE_NO_VENDOR_STOCK")
    void testHinchStoreNoInventoryRowsGivesExplicitReason() {
        // Product platformProductNoRows has zero rows in hinch_store_vendor_inventory
        ProductAvailability avail = productAvailabilityService.getAvailability(platformProductNoRows);
        assertThat(avail.getAvailableQty()).isEqualTo(0);
        assertThat(avail.isInStock()).isFalse();
        assertThat(avail.getReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");
    }

    @Test
    @DisplayName("Cart add: over limit rejected, at limit works, explicit reason shown")
    void testCartAddOverLimitRejectedAndAtLimitWorks() {
        // 1. Adding 16 for vendor product (available 15) -> rejected with available count
        CartItemRequest overLimitReq = CartItemRequest.builder().productId(vendorProduct.getProductId()).quantity(16).build();
        assertThatThrownBy(() -> cartService.addItem(testCustomer.getCustomerId(), overLimitReq))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("exceeds available stock (available: 15)");

        // 2. Adding 15 for vendor product -> succeeds
        CartItemRequest atLimitReq = CartItemRequest.builder().productId(vendorProduct.getProductId()).quantity(15).build();
        CartResponse resp = cartService.addItem(testCustomer.getCustomerId(), atLimitReq);
        assertThat(resp).isNotNull();
        assertThat(resp.getItems()).hasSize(1);
        assertThat(resp.getItems().get(0).getQuantity()).isEqualTo(15);
        assertThat(resp.getItems().get(0).getAvailableQty()).isEqualTo(15);

        // Clear cart before adding from another store to prevent StoreMismatchException
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();

        // 3. Adding HinchStore product with no inventory rows -> rejected with UNAVAILABLE_NO_VENDOR_STOCK
        CartItemRequest noRowsReq = CartItemRequest.builder().productId(platformProductNoRows.getProductId()).quantity(1).build();
        assertThatThrownBy(() -> cartService.addItem(testCustomer.getCustomerId(), noRowsReq))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("UNAVAILABLE_NO_VENDOR_STOCK");
    }

    @Test
    @DisplayName("Preview flags unavailable items without deleting them")
    void testPreviewFlagsUnavailableWithoutDeleting() {
        // Setup cart for customer with platformProductWithStock and platformProductNoRows
        platformMarketplaceProperties.setHinchStoreCheckoutEnabled(false);

        Cart cart = cartRepository.save(Cart.builder()
                .customer(testCustomer)
                .store(platformStore)
                .isActive(true)
                .build());

        cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .product(platformProductWithStock)
                .quantity(5)
                .build());

        cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .product(platformProductNoRows)
                .quantity(2)
                .build());

        // 1. Cart View: flags items without deleting
        CartResponse cartResponse = cartService.getCart(testCustomer.getCustomerId());
        assertThat(cartResponse.getItems()).hasSize(2);
        assertThat(cartItemRepository.findByCart_CartId(cart.getCartId())).hasSize(2); // In DB, untouched

        assertThat(cartResponse.getItems().get(0).getAvailableQty()).isEqualTo(65);
        assertThat(cartResponse.getItems().get(0).getReason()).isEqualTo("CHECKOUT_DISABLED");

        assertThat(cartResponse.getItems().get(1).getAvailableQty()).isEqualTo(0);
        assertThat(cartResponse.getItems().get(1).getReason()).isEqualTo("UNAVAILABLE_NO_VENDOR_STOCK");
        assertThat(cartResponse.getCanCheckout()).isFalse();

        // 2. Preview Checkout: flags order cannot be placed
        CheckoutPreviewResponse preview = checkoutService.previewCheckout(
                testCustomer.getCustomerId(),
                CheckoutPreviewRequest.builder().addressId(testAddress.getId()).build()
        );

        assertThat(preview.getItems()).hasSize(2);
        assertThat(preview.getCanPlaceOrder()).isFalse();
        assertThat(preview.getOrderBlockReason())
                .isEqualTo("Order cannot be placed while any item is unavailable.");
        // Still 2 items in database
        assertThat(cartItemRepository.findByCart_CartId(cart.getCartId())).hasSize(2);
    }

    @Test
    @DisplayName("20-item cart query count stays flat measured with Hibernate Statistics")
    void testTwentyItemCartQueryCountMeasuredWithHibernateStatistics() {
        List<Product> products = new ArrayList<>();
        List<Integer> productIds = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            Product p = productRepository.save(Product.builder()
                    .productId(1000 + i)
                    .store(platformStore)
                    .title("Platform Item #" + i)
                    .slug("platform-item-" + i)
                    .price(new BigDecimal("99.00"))
                    .unit("pcs")
                    .stockQty(10)
                    .active(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .build());
            products.add(p);
            productIds.add(p.getProductId());

            // Add 1 inventory row for each
            jdbcTemplate.update("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) VALUES (?, ?, ?, ?, ?)",
                    p.getProductId(), vendorStore1.getStoreId(), 50, 5, true);
        }

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        // Batch availability check for all 20 products
        long prepCountBefore = statistics.getPrepareStatementCount();
        Map<Integer, ProductAvailability> availMap = productAvailabilityService.getAvailabilityBatch(products);
        long prepCountAfter = statistics.getPrepareStatementCount();

        assertThat(availMap).hasSize(20);
        for (Product p : products) {
            ProductAvailability pa = availMap.get(p.getProductId());
            assertThat(pa).isNotNull();
            assertThat(pa.getAvailableQty()).isEqualTo(45); // 50 - 5
        }

        // Exactly 1 SQL query executed to hinch_store_vendor_inventory for all 20 products
        long statementsExecuted = prepCountAfter - prepCountBefore;
        assertThat(statementsExecuted)
                .withFailMessage("Expected exactly 1 query to fetch stock for 20 platform products, but got " + statementsExecuted)
                .isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Product list query count stays flat (batched stock) for 20 platform products")
    void testProductListEndpointQueryCountStaysFlat() {
        for (int i = 1; i <= 20; i++) {
            Product p = productRepository.save(Product.builder()
                    .productId(2000 + i)
                    .store(platformStore)
                    .title("List Product #" + i)
                    .slug("list-product-" + i)
                    .price(new BigDecimal("120.00"))
                    .unit("pcs")
                    .stockQty(10)
                    .active(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .build());
            jdbcTemplate.update("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) VALUES (?, ?, ?, ?, ?)",
                    p.getProductId(), vendorStore1.getStoreId(), 50, 10, true);
        }

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        long prepBefore = statistics.getPrepareStatementCount();
        ApiResponse<List<ProductResponse>> response = productService.getAll(null, null, null, null, null, null, null, "newest", 1, 20);
        long prepAfter = statistics.getPrepareStatementCount();

        assertThat(response.getData()).hasSize(20);
        assertThat(response.getData().get(0).getStockQty()).isEqualTo(40);

        long statementsExecuted = prepAfter - prepBefore;
        // 1 count query + 1 select products page query + 1 batched availability query (<= 5 queries max, strictly flat O(1), no N+1)
        assertThat(statementsExecuted)
                .withFailMessage("Expected flat query count (<= 5 queries) for 20 products in list, but got " + statementsExecuted)
                .isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Product category query count stays flat (batched stock) for 20 platform products")
    void testProductCategoryEndpointQueryCountStaysFlat() {
        Category cat = categoryRepository.save(Category.builder()
                .name("Steel Category")
                .slug("steel-category")
                .active(true)
                .build());
        Subcategory subcat = subcategoryRepository.save(Subcategory.builder()
                .category(cat)
                .name("Steel Subcategory")
                .slug("steel-subcategory")
                .active(true)
                .build());

        for (int i = 1; i <= 20; i++) {
            Product p = productRepository.save(Product.builder()
                    .productId(3000 + i)
                    .store(platformStore)
                    .subcategory(subcat)
                    .title("Cat Product #" + i)
                    .slug("cat-product-" + i)
                    .price(new BigDecimal("150.00"))
                    .unit("pcs")
                    .stockQty(10)
                    .active(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .build());
            jdbcTemplate.update("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) VALUES (?, ?, ?, ?, ?)",
                    p.getProductId(), vendorStore1.getStoreId(), 60, 10, true);
        }

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        long prepBefore = statistics.getPrepareStatementCount();
        ApiResponse<List<ProductResponse>> response = productService.getAll(cat.getCategoryId(), null, null, null, null, null, null, "newest", 1, 20);
        long prepAfter = statistics.getPrepareStatementCount();

        assertThat(response.getData()).hasSize(20);
        assertThat(response.getData().get(0).getStockQty()).isEqualTo(50);

        long statementsExecuted = prepAfter - prepBefore;
        // 1 count query + 1 select products page query + 1 batched availability query = 3 queries max (no N+1)
        assertThat(statementsExecuted)
                .withFailMessage("Expected flat query count (<= 3 queries) for 20 products in category, but got " + statementsExecuted)
                .isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Product search query count stays flat (batched stock) for 20 platform products")
    void testProductSearchEndpointQueryCountStaysFlat() {
        for (int i = 1; i <= 20; i++) {
            Product p = productRepository.save(Product.builder()
                    .productId(4000 + i)
                    .store(platformStore)
                    .title("Searchable Cement Bag #" + i)
                    .slug("searchable-cement-" + i)
                    .price(new BigDecimal("200.00"))
                    .unit("bag")
                    .stockQty(10)
                    .active(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .build());
            jdbcTemplate.update("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) VALUES (?, ?, ?, ?, ?)",
                    p.getProductId(), vendorStore1.getStoreId(), 70, 20, true);
        }

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        long prepBefore = statistics.getPrepareStatementCount();
        ApiResponse<List<ProductResponse>> response = productService.getAll(null, null, "Searchable Cement", null, null, null, null, "newest", 1, 20);
        long prepAfter = statistics.getPrepareStatementCount();

        assertThat(response.getData()).hasSize(20);
        assertThat(response.getData().get(0).getStockQty()).isEqualTo(50);

        long statementsExecuted = prepAfter - prepBefore;
        // 1 count query + 1 select products page query + 1 batched availability query = 3 queries max (no N+1)
        assertThat(statementsExecuted)
                .withFailMessage("Expected flat query count (<= 3 queries) for 20 products in search, but got " + statementsExecuted)
                .isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Hot-deals endpoint query count stays flat (batched stock) with no N+1 for 20 deals")
    void testHotDealsEndpointQueryCountStaysFlat() {
        for (int i = 1; i <= 20; i++) {
            Product p = productRepository.save(Product.builder()
                    .productId(5000 + i)
                    .store(platformStore)
                    .title("Hot Deal Platform Product #" + i)
                    .slug("hot-deal-prod-" + i)
                    .price(new BigDecimal("300.00"))
                    .unit("pcs")
                    .stockQty(10)
                    .active(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .build());

            jdbcTemplate.update("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) VALUES (?, ?, ?, ?, ?)",
                    p.getProductId(), vendorStore1.getStoreId(), 80, 30, true);

            hotDealRepository.save(HotDeal.builder()
                    .product(p)
                    .displayOrder(i)
                    .active(true)
                    .build());
        }

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        long prepBefore = statistics.getPrepareStatementCount();
        HomeHotDealsResponse response = hotDealService.getActiveHotDealsForHome();
        long prepAfter = statistics.getPrepareStatementCount();

        assertThat(response.getProducts()).hasSize(20);
        assertThat(response.getProducts().get(0).getStockQty()).isEqualTo(50);

        long statementsExecuted = prepAfter - prepBefore;
        // 1 hot deals query + 1 batched inventory query = 2 queries max (strictly NO N+1)
        assertThat(statementsExecuted)
                .withFailMessage("Expected flat query count (<= 2 queries) for 20 hot deals, but got " + statementsExecuted)
                .isLessThanOrEqualTo(2);
    }
}
