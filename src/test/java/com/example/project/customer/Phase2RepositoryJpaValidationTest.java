package com.example.project.customer;

import com.example.project.customer.entity.*;
import com.example.project.customer.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("dev")
@EnabledIf("isDockerAvailable")
public class Phase2RepositoryJpaValidationTest {

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
            .withDatabaseName("hinchmart_jpa_test")
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
        }
    }

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.google.firebase.FirebaseApp firebaseApp;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.google.firebase.auth.FirebaseAuth firebaseAuth;

    @Autowired(required = false)
    private PaymentRepository paymentRepository;

    @Autowired(required = false)
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;

    @Autowired(required = false)
    private StoreRepository storeRepository;

    @Autowired(required = false)
    private ProductRepository productRepository;

    @Autowired(required = false)
    private CustomerRepository customerRepository;

    @Autowired(required = false)
    private SellerRepository sellerRepository;

    @Autowired(required = false)
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private org.springframework.core.env.Environment environment;

    @org.junit.jupiter.api.BeforeEach
    void logProperties() {
        System.out.println(">>> EFFECTIVE spring.jpa.hibernate.ddl-auto: " + environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        System.out.println(">>> EFFECTIVE spring.flyway.enabled: " + environment.getProperty("spring.flyway.enabled"));
    }

    @Test
    @Transactional
    @DisplayName("1. Repository @Query: claimRefundAtomic validates against Hibernate & updates row atomically")
    void testClaimRefundAtomic() {
        Customer c = customerRepository.save(Customer.builder().name("Refund Customer").phone("9999988888").build());
        Payment p = paymentRepository.save(Payment.builder()
                .customer(c)
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status("CAPTURED")
                .razorpayOrderId("order_test_claim_123")
                .build());

        // Call claimRefundAtomic
        int updated = paymentRepository.claimRefundAtomic(p.getPaymentId());
        assertThat(updated).isEqualTo(1);

        if (entityManager != null) {
            entityManager.clear();
        }

        Payment reloaded = paymentRepository.findById(p.getPaymentId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("REFUND_IN_PROGRESS");

        // Idempotency check: Calling claimRefundAtomic again on the same claimed payment must affect 0 rows
        int secondUpdate = paymentRepository.claimRefundAtomic(p.getPaymentId());
        assertThat(secondUpdate).isEqualTo(0);
    }

    @Test
    @Transactional
    @DisplayName("2. Repository @Query: transitionInventoryState validates against Hibernate & ensures conditional transition")
    void testTransitionInventoryState() {
        Customer c = customerRepository.save(Customer.builder().name("Order Customer").phone("9999977777").build());
        Seller seller = sellerRepository.save(Seller.builder().name("Store Seller").companyName("Store Co").email("seller_trans@test.com").phone("9999911111").build());
        Store s = storeRepository.save(Store.builder().seller(seller).name("Test Store").slug("test-store-transition").status(StoreStatus.ACTIVE).build());
        Order o = orderRepository.save(Order.builder()
                .customer(c)
                .store(s)
                .orderNumber("ORD-TRANS-001")
                .orderStatus("PLACED")
                .paymentStatus("PENDING")
                .commissionRate(new BigDecimal("5.00"))
                .inventoryState(OrderInventoryState.RESERVED)
                .totalAmount(new BigDecimal("500.00"))
                .build());

        // Transition RESERVED -> COMMITTED
        int rows = orderRepository.transitionInventoryState(o.getOrderId(), OrderInventoryState.RESERVED, OrderInventoryState.COMMITTED);
        assertThat(rows).isEqualTo(1);

        if (entityManager != null) {
            entityManager.clear();
        }

        Order reloaded = orderRepository.findById(o.getOrderId()).orElseThrow();
        assertThat(reloaded.getInventoryState()).isEqualTo(OrderInventoryState.COMMITTED);

        // Attempting with wrong expected state (RESERVED) must fail (0 rows)
        int staleAttempt = orderRepository.transitionInventoryState(o.getOrderId(), OrderInventoryState.RESERVED, OrderInventoryState.RELEASED);
        assertThat(staleAttempt).isEqualTo(0);
    }

    @Test
    @Transactional
    @DisplayName("3. Repository @Query: atomicReserve, atomicCommit, atomicRelease, atomicRestock, and availability SUM validate against Hibernate")
    void testConsignmentInventoryQueriesAndSum() {
        Seller seller = sellerRepository.save(Seller.builder().name("Inv Seller").companyName("Inventory Vendor").email("seller_inv@test.com").phone("9999922222").build());
        Store vendorStore = storeRepository.save(Store.builder().seller(seller).name("Consignment Store").slug("consignment-store").status(StoreStatus.ACTIVE).build());
        Product product = productRepository.save(Product.builder().store(vendorStore).title("Cement Bag").slug("cement-bag").unit("pcs").price(new BigDecimal("350.00")).active(true).build());

        HinchStoreVendorInventory inv = hinchStoreVendorInventoryRepository.save(HinchStoreVendorInventory.builder()
                .product(product)
                .vendorStore(vendorStore)
                .qty(20)
                .reservedQty(5)
                .isActive(true)
                .build());

        Integer pid = product.getProductId();
        Integer sid = vendorStore.getStoreId();

        // 3a. atomicReserve: reserve 5 units (available: 20 - 5 = 15 >= 5)
        int r1 = hinchStoreVendorInventoryRepository.atomicReserve(pid, sid, 5);
        assertThat(r1).isEqualTo(1);

        // Exceed available units (remaining: 20 - 10 = 10, requesting 15)
        int rExceed = hinchStoreVendorInventoryRepository.atomicReserve(pid, sid, 15);
        assertThat(rExceed).isEqualTo(0);

        // 3b. atomicCommit: commit 4 reserved units
        int c1 = hinchStoreVendorInventoryRepository.atomicCommit(pid, sid, 4);
        assertThat(c1).isEqualTo(1);

        // 3c. atomicRelease: release 3 reserved units
        int rel1 = hinchStoreVendorInventoryRepository.atomicRelease(pid, sid, 3);
        assertThat(rel1).isEqualTo(1);

        // 3d. atomicRestock: restock 10 units
        int restock1 = hinchStoreVendorInventoryRepository.atomicRestock(pid, sid, 10);
        assertThat(restock1).isEqualTo(1);

        // 3e. availability SUM query
        int availableSum = hinchStoreVendorInventoryRepository.getAvailableStockForProduct(pid);
        assertThat(availableSum).isEqualTo(23);
    }

    @Test
    @Transactional
    @DisplayName("4. Repository @Query: findSellerOrders & findSellerOrdersByStatus validate against Hibernate")
    void testFindSellerOrdersQueries() {
        Seller seller = sellerRepository.save(Seller.builder().name("Order Seller").companyName("Order Vendor").email("seller_order@test.com").phone("9999933333").build());
        Store store = storeRepository.save(Store.builder().seller(seller).name("Seller Store").slug("seller-store-orders").status(StoreStatus.ACTIVE).build());
        Customer c = customerRepository.save(Customer.builder().name("Buyer").phone("9999966666").build());

        Order o = orderRepository.save(Order.builder()
                .customer(c)
                .store(store)
                .fulfillmentStore(store)
                .orderNumber("ORD-SELLER-001")
                .orderStatus("CONFIRMED")
                .paymentStatus("PAID")
                .commissionRate(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("1200.00"))
                .build());

        Page<Order> allSellerOrders = orderRepository.findSellerOrders(seller.getSellerId(), PageRequest.of(0, 10));
        assertThat(allSellerOrders.getContent()).extracting(Order::getOrderNumber).contains("ORD-SELLER-001");

        Page<Order> confirmedOrders = orderRepository.findSellerOrdersByStatus(seller.getSellerId(), "CONFIRMED", PageRequest.of(0, 10));
        assertThat(confirmedOrders.getContent()).extracting(Order::getOrderNumber).contains("ORD-SELLER-001");

        Page<Order> deliveredOrders = orderRepository.findSellerOrdersByStatus(seller.getSellerId(), "DELIVERED", PageRequest.of(0, 10));
        assertThat(deliveredOrders.getContent()).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("5. Repository @Query: findExpiredUnpaidHinchOrders validates against Hibernate")
    void testFindExpiredUnpaidHinchOrdersQuery() {
        Seller seller = sellerRepository.save(Seller.builder().name("Platform Seller").companyName("Platform Co").email("seller_platform@test.com").phone("9999944444").build());
        Store platformStore = storeRepository.save(Store.builder().seller(seller).name("HinchPlatform Central").slug("hinch-platform").status(StoreStatus.ACTIVE).build());
        Customer c = customerRepository.save(Customer.builder().name("Timeout Customer").phone("9999955555").build());

        Order expiredOrder = Order.builder()
                .customer(c)
                .store(platformStore)
                .orderNumber("ORD-EXPIRED-001")
                .orderStatus("PLACED")
                .paymentStatus("PENDING")
                .commissionRate(new BigDecimal("5.00"))
                .inventoryState(OrderInventoryState.RESERVED)
                .totalAmount(new BigDecimal("800.00"))
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .build();
        orderRepository.save(expiredOrder);

        List<Order> found = orderRepository.findExpiredUnpaidHinchOrders(platformStore.getStoreId(), LocalDateTime.now().minusMinutes(15));
        assertThat(found).extracting(Order::getOrderNumber).contains("ORD-EXPIRED-001");
    }
}
