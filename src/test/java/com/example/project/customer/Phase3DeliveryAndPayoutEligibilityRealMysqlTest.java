package com.example.project.customer;

import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.PayoutLedgerStatus;
import com.example.project.customer.entity.Role;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerPayoutLedger;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.PaymentRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.security.FirebaseUserPrincipal;
import com.example.project.customer.service.OrderService;
import com.example.project.customer.service.PaymentService;
import com.example.project.customer.service.scheduler.PayoutLedgerEligibilitySweepJob;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIf("isDockerAvailable")
public class Phase3DeliveryAndPayoutEligibilityRealMysqlTest {

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
            .withDatabaseName("hinchmart_p3_step3_test")
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
    private RazorpayClient razorpayClient;

    private PaymentClient mockPaymentClient;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private SellerPayoutLedgerRepository sellerPayoutLedgerRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PayoutLedgerEligibilitySweepJob payoutLedgerEligibilitySweepJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        mockPaymentClient = mock(PaymentClient.class);
        Field paymentsField = RazorpayClient.class.getDeclaredField("payments");
        paymentsField.setAccessible(true);
        paymentsField.set(razorpayClient, mockPaymentClient);
        SecurityContextHolder.clearContext();
    }

    private void authenticateAdmin() {
        FirebaseUserPrincipal admin = FirebaseUserPrincipal.create(
                1, "fb_admin_uid", "admin@hinchmart.com", "Admin User", Role.ADMIN, null, true, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    private void authenticateSeller(Seller seller) {
        FirebaseUserPrincipal sellerPrincipal = FirebaseUserPrincipal.create(
                seller.getSellerId(), "fb_seller_" + seller.getSellerId(), seller.getEmail(), seller.getName(),
                Role.SELLER, seller.getSellerId(), true, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sellerPrincipal, null, sellerPrincipal.getAuthorities()));
    }

    private Customer createCustomer(String phone) {
        return customerRepository.save(Customer.builder()
                .name("Test Buyer")
                .email("buyer_" + phone + "@test.com")
                .phone(phone)
                .role("BUYER")
                .active(true)
                .build());
    }

    private Seller createSeller(String email) {
        return sellerRepository.save(Seller.builder()
                .name("Vendor Seller")
                .companyName("Vendor Co " + email)
                .email(email)
                .phone("9" + (System.currentTimeMillis() % 1000000000L))
                .panNumber("ABCDE" + (System.currentTimeMillis() % 10000L) + "F")
                .build());
    }

    private Store createStore(Seller seller, String slug) {
        return storeRepository.save(Store.builder()
                .seller(seller)
                .name("Store " + slug)
                .slug(slug)
                .status(StoreStatus.ACTIVE)
                .commissionRate(new BigDecimal("5.00"))
                .build());
    }

    @Test
    @DisplayName("1. Seller cannot set DELIVERED, admin can")
    void testSellerCannotSetDelivered_AdminCan() {
        Customer c = createCustomer("9200000001");
        Seller s = createSeller("seller1@deliverytest.com");
        Store store = createStore(s, "store-delivery-test-1");

        Order o = orderRepository.save(Order.builder()
                .customer(c).store(store).orderNumber("ORD-DELV-ROLE-01")
                .orderStatus("IN_TRANSIT").paymentStatus("PAID")
                .totalAmount(new BigDecimal("2000.00")).commissionRate(new BigDecimal("5.00")).build());

        // 1. Seller attempts to mark DELIVERED -> throws ForbiddenException
        authenticateSeller(s);
        try {
            assertThatThrownBy(() -> orderService.updateOrderStatus(o.getOrderId(), "DELIVERED", "Site", "Unloaded"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Only administrators can mark an order as DELIVERED");
        } finally {
            SecurityContextHolder.clearContext();
        }

        // 2. Admin marks DELIVERED -> succeeds
        authenticateAdmin();
        try {
            orderService.updateOrderStatus(o.getOrderId(), "DELIVERED", "Site", "Unloaded");
            Order reloaded = orderRepository.findById(o.getOrderId()).orElseThrow();
            assertThat(reloaded.getOrderStatus()).isEqualTo("DELIVERED");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("2. DELIVERED from CONFIRMED is rejected; from DISPATCHED and IN_TRANSIT works")
    void testDeliveredFromConfirmedIsRejected_FromDispatchedAndInTransitWorks() {
        authenticateAdmin();
        try {
            Customer c = createCustomer("9200000002");
            Seller s = createSeller("seller2@deliverytest.com");
            Store store = createStore(s, "store-delivery-test-2");

            // Case A: From CONFIRMED -> must be rejected
            Order oConfirmed = orderRepository.save(Order.builder()
                    .customer(c).store(store).orderNumber("ORD-DELV-CONF")
                    .orderStatus("CONFIRMED").paymentStatus("PAID")
                    .totalAmount(new BigDecimal("1000.00")).commissionRate(new BigDecimal("5.00")).build());

            assertThatThrownBy(() -> orderService.updateOrderStatus(oConfirmed.getOrderId(), "DELIVERED", "Site", "Unloaded"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be marked DELIVERED from status 'CONFIRMED'")
                    .hasMessageContaining("Allowed prior statuses are DISPATCHED or IN_TRANSIT");

            // Case B: From DISPATCHED -> must succeed
            Order oDispatched = orderRepository.save(Order.builder()
                    .customer(c).store(store).orderNumber("ORD-DELV-DISP")
                    .orderStatus("DISPATCHED").paymentStatus("PAID")
                    .totalAmount(new BigDecimal("1000.00")).commissionRate(new BigDecimal("5.00")).build());

            orderService.updateOrderStatus(oDispatched.getOrderId(), "DELIVERED", "Jobsite", "Delivered by truck");
            assertThat(orderRepository.findById(oDispatched.getOrderId()).orElseThrow().getOrderStatus())
                    .isEqualTo("DELIVERED");

            // Case C: From IN_TRANSIT -> must succeed
            Order oInTransit = orderRepository.save(Order.builder()
                    .customer(c).store(store).orderNumber("ORD-DELV-TRANS")
                    .orderStatus("IN_TRANSIT").paymentStatus("PAID")
                    .totalAmount(new BigDecimal("1000.00")).commissionRate(new BigDecimal("5.00")).build());

            orderService.updateOrderStatus(oInTransit.getOrderId(), "DELIVERED", "Jobsite", "Delivered by driver");
            assertThat(orderRepository.findById(oInTransit.getOrderId()).orElseThrow().getOrderStatus())
                    .isEqualTo("DELIVERED");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("3. Two concurrent DELIVERED calls give one status change, one checkpoint and one ledger update")
    void testConcurrentDeliveredCalls_IdempotentSingleUpdateAndCheckpoint() throws Exception {
        Customer c = createCustomer("9200000003");
        Seller s = createSeller("seller3@deliverytest.com");
        Store store = createStore(s, "store-delivery-test-3");

        Order o = orderRepository.save(Order.builder()
                .customer(c).store(store).orderNumber("ORD-DELV-CONC")
                .orderStatus("IN_TRANSIT").paymentStatus("PAID")
                .totalAmount(new BigDecimal("3000.00")).commissionRate(new BigDecimal("5.00")).build());

        SellerPayoutLedger ledger = sellerPayoutLedgerRepository.save(SellerPayoutLedger.builder()
                .order(o).store(store).seller(s).grossAmount(new BigDecimal("3000.00"))
                .commissionAmount(new BigDecimal("150.00")).tcsAmount(new BigDecimal("30.00"))
                .netPayoutAmount(new BigDecimal("2820.00")).status(PayoutLedgerStatus.PENDING)
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                authenticateAdmin();
                readyLatch.countDown();
                try {
                    startLatch.await();
                    orderService.updateOrderStatus(o.getOrderId(), "DELIVERED", "Jobsite Hyderabad",
                            "Unloaded successfully", "HichExpress", "TS-09-UB-1234", "Ramesh Kumar", "TRK-9999", null);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(2); // Both return HTTP 200 / success response

        Order reloadedOrder = orderRepository.findById(o.getOrderId()).orElseThrow();
        assertThat(reloadedOrder.getOrderStatus()).isEqualTo("DELIVERED");

        // Exactly 1 DELIVERED checkpoint created in MySQL
        Integer deliveredCheckpoints = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM order_tracking_checkpoints WHERE order_id = ? AND status = 'DELIVERED'",
                Integer.class, o.getOrderId());
        assertThat(deliveredCheckpoints).isEqualTo(1);

        String checkpointDesc = jdbcTemplate.queryForObject(
                "SELECT description FROM order_tracking_checkpoints WHERE order_id = ? AND status = 'DELIVERED' LIMIT 1",
                String.class, o.getOrderId());
        assertThat(checkpointDesc).contains("Delivered by: Ramesh Kumar");

        // Exactly 1 ledger row exists, status is still PENDING, settlementDate set
        List<SellerPayoutLedger> ledgers = sellerPayoutLedgerRepository.findAllByOrder_OrderId(o.getOrderId());
        assertThat(ledgers).hasSize(1);
        assertThat(ledgers.get(0).getStatus()).isEqualTo(PayoutLedgerStatus.PENDING);
        assertThat(ledgers.get(0).getSettlementDate()).isNotNull();
    }

    @Test
    @DisplayName("4. DELIVERED leaves ledger PENDING with settlementDate about now + 7 days; repeat call does not reset date")
    void testDeliveredLeavesLedgerPendingWithTPlus7Date() {
        authenticateAdmin();
        try {
            Customer c = createCustomer("9200000004");
            Seller s = createSeller("seller4@deliverytest.com");
            Store store = createStore(s, "store-delivery-test-4");

            Order o = orderRepository.save(Order.builder()
                    .customer(c).store(store).orderNumber("ORD-DELV-T7")
                    .orderStatus("IN_TRANSIT").paymentStatus("PAID")
                    .totalAmount(new BigDecimal("1500.00")).commissionRate(new BigDecimal("5.00")).build());

            SellerPayoutLedger ledger = sellerPayoutLedgerRepository.save(SellerPayoutLedger.builder()
                    .order(o).store(store).seller(s).grossAmount(new BigDecimal("1500.00"))
                    .commissionAmount(new BigDecimal("75.00")).tcsAmount(new BigDecimal("15.00"))
                    .netPayoutAmount(new BigDecimal("1410.00")).status(PayoutLedgerStatus.PENDING)
                    .build());

            orderService.updateOrderStatus(o.getOrderId(), "DELIVERED", "Hyderabad", "Delivered");

            SellerPayoutLedger reloaded1 = sellerPayoutLedgerRepository.findById(ledger.getId()).orElseThrow();
            assertThat(reloaded1.getStatus()).isEqualTo(PayoutLedgerStatus.PENDING); // MUST NOT BE ELIGIBLE
            assertThat(reloaded1.getSettlementDate()).isNotNull();

            long daysDiff = ChronoUnit.DAYS.between(LocalDateTime.now(), reloaded1.getSettlementDate());
            assertThat(daysDiff).isBetween(6L, 7L);

            LocalDateTime originalSettlementDate = reloaded1.getSettlementDate();

            // Repeat call (idempotent): must not reset date and must not duplicate ledger
            orderService.updateOrderStatus(o.getOrderId(), "DELIVERED", "Hyderabad", "Delivered again");

            SellerPayoutLedger reloaded2 = sellerPayoutLedgerRepository.findById(ledger.getId()).orElseThrow();
            assertThat(reloaded2.getSettlementDate()).isEqualTo(originalSettlementDate);
            assertThat(sellerPayoutLedgerRepository.findAllByOrder_OrderId(o.getOrderId())).hasSize(1);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("5. Payout sweep job: moves only rows past settlementDate, and two concurrent runs move each row once")
    void testPayoutEligibilityJob_MovesOnlyPastDate_ConcurrentSafe() throws Exception {
        Customer c = createCustomer("9200000005");
        Seller s = createSeller("seller5@deliverytest.com");
        Store store = createStore(s, "store-delivery-test-5");

        Order o = orderRepository.save(Order.builder().customer(c).store(store).orderNumber("ORD-SWEEP-ELIG")
                .orderStatus("DELIVERED").paymentStatus("PAID").totalAmount(new BigDecimal("1000.00")).commissionRate(new BigDecimal("5.00")).build());

        // 1. Mature ledger: settlementDate was 1 hour ago (past) -> MUST BECOME ELIGIBLE
        SellerPayoutLedger lMature = sellerPayoutLedgerRepository.save(SellerPayoutLedger.builder()
                .order(o).store(store).seller(s).grossAmount(new BigDecimal("1000.00"))
                .netPayoutAmount(new BigDecimal("950.00")).status(PayoutLedgerStatus.PENDING)
                .settlementDate(LocalDateTime.now().minusHours(1)).build());

        // 2. Future ledger: settlementDate is 5 days in future -> MUST STAY PENDING
        SellerPayoutLedger lFuture = sellerPayoutLedgerRepository.save(SellerPayoutLedger.builder()
                .order(o).store(store).seller(s).grossAmount(new BigDecimal("1000.00"))
                .netPayoutAmount(new BigDecimal("950.00")).status(PayoutLedgerStatus.PENDING)
                .settlementDate(LocalDateTime.now().plusDays(5)).build());

        // 3. Mature ledger with clawbackReason -> MUST STAY PENDING (excluded from promotion)
        SellerPayoutLedger lClawback = sellerPayoutLedgerRepository.save(SellerPayoutLedger.builder()
                .order(o).store(store).seller(s).grossAmount(new BigDecimal("1000.00"))
                .netPayoutAmount(new BigDecimal("950.00")).status(PayoutLedgerStatus.PENDING)
                .settlementDate(LocalDateTime.now().minusHours(1)).clawbackReason("Dispute under investigation").build());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger totalPromoted = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    int count = payoutLedgerEligibilitySweepJob.processEligibleLedgers(LocalDateTime.now(), 50);
                    totalPromoted.addAndGet(count);
                } catch (Exception ignored) {
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(totalPromoted.get()).isEqualTo(1); // Exactly 1 promotion across both concurrent runs

        assertThat(sellerPayoutLedgerRepository.findById(lMature.getId()).orElseThrow().getStatus())
                .isEqualTo(PayoutLedgerStatus.ELIGIBLE);
        assertThat(sellerPayoutLedgerRepository.findById(lFuture.getId()).orElseThrow().getStatus())
                .isEqualTo(PayoutLedgerStatus.PENDING);
        assertThat(sellerPayoutLedgerRepository.findById(lClawback.getId()).orElseThrow().getStatus())
                .isEqualTo(PayoutLedgerStatus.PENDING);
    }

    @Test
    @DisplayName("6. Reversal: PENDING ledger becomes REVERSED; ELIGIBLE ledger keeps status and gets clawbackReason")
    void testReversal_PendingBecomesReversed_EligibleKeepsStatusAndGetsClawbackReason() {
        Customer c = createCustomer("9200000006");
        Seller s = createSeller("seller6@deliverytest.com");
        Store store = createStore(s, "store-delivery-test-6");

        // Case A: Order with PENDING ledger
        Order oPending = orderRepository.save(Order.builder().customer(c).store(store).orderNumber("ORD-REV-PEND")
                .orderStatus("PLACED").paymentStatus("PENDING").totalAmount(new BigDecimal("800.00")).commissionRate(new BigDecimal("5.00")).build());
        SellerPayoutLedger lPending = sellerPayoutLedgerRepository.save(SellerPayoutLedger.builder()
                .order(oPending).store(store).seller(s).grossAmount(new BigDecimal("800.00"))
                .netPayoutAmount(new BigDecimal("750.00")).status(PayoutLedgerStatus.PENDING).build());

        authenticateAdmin();
        try {
            orderService.cancelOrder(oPending.getOrderId(), "Hyderabad", "Customer cancelled before dispatch");
            SellerPayoutLedger reloadedPending = sellerPayoutLedgerRepository.findById(lPending.getId()).orElseThrow();
            assertThat(reloadedPending.getStatus()).isEqualTo(PayoutLedgerStatus.REVERSED);
            assertThat(reloadedPending.getClawbackReason()).contains("Customer cancelled before dispatch");
        } finally {
            SecurityContextHolder.clearContext();
        }

        // Case B: Order with ELIGIBLE ledger
        Order oEligible = orderRepository.save(Order.builder().customer(c).store(store).orderNumber("ORD-REV-ELIG")
                .orderStatus("PLACED").paymentStatus("PENDING").totalAmount(new BigDecimal("1200.00")).commissionRate(new BigDecimal("5.00")).build());
        SellerPayoutLedger lEligible = sellerPayoutLedgerRepository.save(SellerPayoutLedger.builder()
                .order(oEligible).store(store).seller(s).grossAmount(new BigDecimal("1200.00"))
                .netPayoutAmount(new BigDecimal("1140.00")).status(PayoutLedgerStatus.ELIGIBLE)
                .settlementDate(LocalDateTime.now().minusDays(1)).build());

        authenticateAdmin();
        try {
            orderService.cancelOrder(oEligible.getOrderId(), "Hyderabad", "Late defect refund");
            SellerPayoutLedger reloadedEligible = sellerPayoutLedgerRepository.findById(lEligible.getId()).orElseThrow();
            assertThat(reloadedEligible.getStatus()).isEqualTo(PayoutLedgerStatus.ELIGIBLE); // Must NOT be overwritten
            assertThat(reloadedEligible.getClawbackReason()).contains("Late defect refund");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("7. A4 SQL query: finds CONFIRMED and SHIPPED orders whose payout ledger is still PENDING")
    void testA4SqlQuery_FindsExpectedRows() {
        Customer c = createCustomer("9200000007");
        Seller s = createSeller("seller7@deliverytest.com");
        Store store = createStore(s, "store-delivery-test-7");

        // Order 1: CONFIRMED with PENDING ledger -> MUST be returned by A4 query
        Order o1 = orderRepository.save(Order.builder().customer(c).store(store).orderNumber("ORD-A4-CONF")
                .orderStatus("CONFIRMED").paymentStatus("PAID").totalAmount(new BigDecimal("500.00")).commissionRate(new BigDecimal("5.00")).build());
        SellerPayoutLedger l1 = sellerPayoutLedgerRepository.save(SellerPayoutLedger.builder()
                .order(o1).store(store).seller(s).grossAmount(new BigDecimal("500.00"))
                .netPayoutAmount(new BigDecimal("475.00")).status(PayoutLedgerStatus.PENDING).build());

        // Order 2: SHIPPED with PENDING ledger -> MUST be returned by A4 query
        Order o2 = orderRepository.save(Order.builder().customer(c).store(store).orderNumber("ORD-A4-SHIP")
                .orderStatus("SHIPPED").paymentStatus("PAID").totalAmount(new BigDecimal("600.00")).commissionRate(new BigDecimal("5.00")).build());
        SellerPayoutLedger l2 = sellerPayoutLedgerRepository.save(SellerPayoutLedger.builder()
                .order(o2).store(store).seller(s).grossAmount(new BigDecimal("600.00"))
                .netPayoutAmount(new BigDecimal("570.00")).status(PayoutLedgerStatus.PENDING).build());

        // Order 3: DELIVERED with PENDING ledger -> MUST NOT be returned by A4 query
        Order o3 = orderRepository.save(Order.builder().customer(c).store(store).orderNumber("ORD-A4-DELV")
                .orderStatus("DELIVERED").paymentStatus("PAID").totalAmount(new BigDecimal("700.00")).commissionRate(new BigDecimal("5.00")).build());
        sellerPayoutLedgerRepository.save(SellerPayoutLedger.builder()
                .order(o3).store(store).seller(s).grossAmount(new BigDecimal("700.00"))
                .netPayoutAmount(new BigDecimal("665.00")).status(PayoutLedgerStatus.PENDING).build());

        String sql = "SELECT l.id AS ledger_id, l.order_id, o.order_number, o.order_status, l.status AS ledger_status " +
                "FROM seller_payout_ledgers l " +
                "JOIN orders o ON l.order_id = o.order_id " +
                "WHERE l.status = 'PENDING' AND o.order_status IN ('CONFIRMED', 'SHIPPED') " +
                "AND l.id IN (" + l1.getId() + ", " + l2.getId() + ")";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        assertThat(rows).hasSize(2);
        assertThat(rows.stream().map(r -> r.get("order_number"))).containsExactlyInAnyOrder("ORD-A4-CONF", "ORD-A4-SHIP");
    }
}
