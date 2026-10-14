package com.example.project.customer;

import com.example.project.customer.dto.PaymentStatusResponse;
import com.example.project.customer.dto.PaymentVerifyRequest;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.Payment;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.PaymentRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.service.PaymentService;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.project.customer.entity.Role;
import com.example.project.customer.security.FirebaseUserPrincipal;
import com.example.project.customer.service.OrderService;
import com.example.project.customer.exception.ForbiddenException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIf("isDockerAvailable")
public class Phase3RefundSafetyRealMysqlTest {

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
            .withDatabaseName("hinchmart_p3_test")
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
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockPaymentClient = mock(PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", mockPaymentClient);
        ReflectionTestUtils.setField(paymentService, "keyId", "rzp_test_mockKey");
        ReflectionTestUtils.setField(paymentService, "keySecret", "mockSecretKey123");
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "mockWebhookSecret123");
    }

    private Customer createCustomer(String phone) {
        return customerRepository.save(Customer.builder()
                .name("Refund Tester")
                .email("refund_" + phone + "@test.com")
                .phone(phone)
                .build());
    }

    private Store createStore(String slug) {
        Seller seller = sellerRepository.save(Seller.builder()
                .name("Seller " + slug)
                .companyName("Company " + slug)
                .email("seller_" + slug + "@test.com")
                .phone("9" + Math.abs(slug.hashCode()))
                .build());
        return storeRepository.save(Store.builder()
                .seller(seller)
                .name("Store " + slug)
                .slug(slug)
                .status(StoreStatus.ACTIVE)
                .build());
    }

    private static String calculateHmacSha256(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("1. Real MySQL: Stale refund recovery query identifies payments older than cutoff and reconciles via fetchAllRefunds")
    void testStaleRecoveryQueryAndReconciliation() throws Exception {
        Customer c = createCustomer("9100000001");
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

        // Stale payment: updated 20 minutes ago
        Payment stalePayment = paymentRepository.save(Payment.builder()
                .customer(c)
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status("REFUND_IN_PROGRESS")
                .razorpayPaymentId("pay_stale_123")
                .razorpayOrderId("order_stale_123")
                .build());
        // Set updatedAt directly to 20 minutes ago in MySQL (bypassing JPA @PreUpdate)
        jdbcTemplate.update("UPDATE payments SET updated_at = DATE_SUB(NOW(), INTERVAL 20 MINUTE) WHERE payment_id = ?",
                stalePayment.getPaymentId());

        // Fresh payment: updated 2 minutes ago
        Payment freshPayment = paymentRepository.save(Payment.builder()
                .customer(c)
                .amount(new BigDecimal("300.00"))
                .currency("INR")
                .status("REFUND_IN_PROGRESS")
                .razorpayPaymentId("pay_fresh_123")
                .razorpayOrderId("order_fresh_123")
                .build());

        // 1a. Assert query returns only the stale payment
        List<Payment> foundStale = paymentRepository.findByStatusAndUpdatedAtBefore("REFUND_IN_PROGRESS", cutoff);
        assertThat(foundStale).extracting(Payment::getPaymentId).contains(stalePayment.getPaymentId());
        assertThat(foundStale).extracting(Payment::getPaymentId).doesNotContain(freshPayment.getPaymentId());

        // 1b. Mock Razorpay fetchAllRefunds returning an existing refund
        Refund mockRefund = mock(Refund.class);
        when(mockRefund.has("id")).thenReturn(true);
        when(mockRefund.get("id")).thenReturn("rfnd_stale_reconciled");
        when(mockPaymentClient.fetchAllRefunds("pay_stale_123")).thenReturn(List.of(mockRefund));

        int reconciled = paymentService.reconcileStaleRefunds(15);
        assertThat(reconciled).isGreaterThanOrEqualTo(1);

        Payment reloaded = paymentRepository.findById(stalePayment.getPaymentId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("REFUNDED");
        assertThat(reloaded.getErrorDescription()).contains("rfnd_stale_reconciled");
    }

    @Test
    @DisplayName("2. Real MySQL: verifyPayment & handleWebhook never overwrite REFUND_IN_PROGRESS or REFUND_PENDING to CAPTURED")
    void testGuardsPreventOverwritingRefundState() throws Exception {
        Customer c = createCustomer("9100000002");
        Store s = createStore("store-guards-test");

        Order o = orderRepository.save(Order.builder()
                .customer(c)
                .store(s)
                .orderNumber("ORD-GUARD-001")
                .orderStatus("PLACED")
                .paymentStatus("PENDING")
                .commissionRate(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("1000.00"))
                .build());

        Payment refundingPayment = paymentRepository.save(Payment.builder()
                .customer(c)
                .order(o)
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status("REFUND_IN_PROGRESS")
                .razorpayOrderId("order_guard_rzp_001")
                .razorpayPaymentId("pay_guard_rzp_001")
                .build());

        Payment pendingPayment = paymentRepository.save(Payment.builder()
                .customer(c)
                .order(o)
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status("REFUND_PENDING")
                .razorpayOrderId("order_guard_rzp_002")
                .razorpayPaymentId("pay_guard_rzp_002")
                .build());

        // Mock Razorpay payment fetch response
        com.razorpay.Payment rzpPaymentMock = mock(com.razorpay.Payment.class);
        when(rzpPaymentMock.get("status")).thenReturn("captured");
        when(rzpPaymentMock.get("amount")).thenReturn(100000);
        when(rzpPaymentMock.has("method")).thenReturn(false);
        when(rzpPaymentMock.has("error_code")).thenReturn(false);
        when(rzpPaymentMock.has("error_description")).thenReturn(false);
        when(mockPaymentClient.fetch("pay_guard_rzp_001")).thenReturn(rzpPaymentMock);
        when(mockPaymentClient.fetch("pay_guard_rzp_002")).thenReturn(rzpPaymentMock);

        // Generate valid signature using test secret
        String sig1 = calculateHmacSha256("order_guard_rzp_001|pay_guard_rzp_001", "mockSecretKey123");
        PaymentVerifyRequest verifyReq1 = PaymentVerifyRequest.builder()
                .orderId(o.getOrderId())
                .razorpayOrderId("order_guard_rzp_001")
                .razorpayPaymentId("pay_guard_rzp_001")
                .razorpaySignature(sig1)
                .build();

        // 2a. Call verifyPayment on REFUND_IN_PROGRESS
        paymentService.verifyPayment(c.getCustomerId(), verifyReq1);
        Payment reloaded1 = paymentRepository.findById(refundingPayment.getPaymentId()).orElseThrow();
        assertThat(reloaded1.getStatus()).isEqualTo("REFUND_IN_PROGRESS");

        // 2b. Call verifyPayment on REFUND_PENDING
        String sig2 = calculateHmacSha256("order_guard_rzp_002|pay_guard_rzp_002", "mockSecretKey123");
        PaymentVerifyRequest verifyReq2 = PaymentVerifyRequest.builder()
                .orderId(o.getOrderId())
                .razorpayOrderId("order_guard_rzp_002")
                .razorpayPaymentId("pay_guard_rzp_002")
                .razorpaySignature(sig2)
                .build();
        paymentService.verifyPayment(c.getCustomerId(), verifyReq2);
        Payment reloaded2 = paymentRepository.findById(pendingPayment.getPaymentId()).orElseThrow();
        assertThat(reloaded2.getStatus()).isEqualTo("REFUND_PENDING");

        // 2c. Webhook event payment.captured for REFUND_IN_PROGRESS
        JSONObject webhookPayload = new JSONObject()
                .put("event", "payment.captured")
                .put("payload", new JSONObject()
                        .put("payment", new JSONObject()
                                .put("entity", new JSONObject()
                                        .put("id", "pay_guard_rzp_001")
                                        .put("order_id", "order_guard_rzp_001")
                                        .put("method", "card"))));
        String webhookSig = calculateHmacSha256(webhookPayload.toString(), "mockWebhookSecret123");

        paymentService.handleWebhook(webhookPayload.toString(), webhookSig);
        Payment reloadedWebhook = paymentRepository.findById(refundingPayment.getPaymentId()).orElseThrow();
        assertThat(reloadedWebhook.getStatus()).isEqualTo("REFUND_IN_PROGRESS");
    }

    @Test
    @DisplayName("3. Real MySQL: Two concurrent threads claiming same refund - only ONE reaches the Razorpay refund mock")
    void testConcurrentRefundClaimsAtomic() throws Exception {
        Customer c = createCustomer("9100000003");
        Store s = createStore("store-race-test");

        Order o = orderRepository.save(Order.builder()
                .customer(c)
                .store(s)
                .orderNumber("ORD-RACE-001")
                .orderStatus("PLACED")
                .paymentStatus("PAID")
                .commissionRate(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("1500.00"))
                .build());

        Payment p = paymentRepository.save(Payment.builder()
                .customer(c)
                .order(o)
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status("CAPTURED")
                .razorpayPaymentId("pay_race_concurrent_123")
                .razorpayOrderId("order_race_concurrent_123")
                .build());

        Refund mockRefund = mock(Refund.class);
        when(mockRefund.has("id")).thenReturn(true);
        when(mockRefund.get("id")).thenReturn("rfnd_race_success");
        when(mockPaymentClient.refund(eq("pay_race_concurrent_123"), any(JSONObject.class))).thenReturn(mockRefund);

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    paymentService.refundOrderPayment(o.getOrderId(), "Concurrent test cancellation");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Ignored or logged
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();
        executor.shutdown();

        // Exactly one thread should have called the Razorpay refund API
        verify(mockPaymentClient, times(1)).refund(eq("pay_race_concurrent_123"), any(JSONObject.class));

        Payment reloaded = paymentRepository.findById(p.getPaymentId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    @DisplayName("4. Real MySQL: Refund status write (REQUIRES_NEW) survives failed gateway call")
    void testStatusWriteSurvivesGatewayFailure() throws Exception {
        Customer c = createCustomer("9100000004");
        Store s = createStore("store-rollback-test");

        Order o = orderRepository.save(Order.builder()
                .customer(c)
                .store(s)
                .orderNumber("ORD-ROLLBACK-001")
                .orderStatus("PLACED")
                .paymentStatus("PAID")
                .commissionRate(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("750.00"))
                .build());

        Payment p = paymentRepository.save(Payment.builder()
                .customer(c)
                .order(o)
                .amount(new BigDecimal("750.00"))
                .currency("INR")
                .status("CAPTURED")
                .razorpayPaymentId("pay_fail_gateway_456")
                .razorpayOrderId("order_fail_gateway_456")
                .build());

        // Make Razorpay refund fail with exception and fetchAllRefunds return empty
        when(mockPaymentClient.refund(eq("pay_fail_gateway_456"), any(JSONObject.class)))
                .thenThrow(new RuntimeException("Gateway connection timeout"));
        when(mockPaymentClient.fetchAllRefunds("pay_fail_gateway_456"))
                .thenReturn(Collections.emptyList());

        // Call refund
        paymentService.refundOrderPayment(o.getOrderId(), "Customer cancellation");

        // Status must have been written as REFUND_PENDING in MySQL and survived
        Payment reloaded = paymentRepository.findById(p.getPaymentId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("REFUND_PENDING");
        assertThat(reloaded.getErrorDescription()).contains("Gateway connection timeout");
    }

    @Test
    @DisplayName("5. Security: User A cannot refund User B's order (ForbiddenException)")
    void testUserCannotRefundAnotherUsersOrder() {
        Customer customerA = createCustomer("9100000005");
        Customer customerB = createCustomer("9100000006");
        Store store = createStore("store-ownership-test");

        Order orderB = orderRepository.save(Order.builder()
                .customer(customerB)
                .store(store)
                .orderNumber("ORD-OWNERSHIP-001")
                .orderStatus("CANCELLED")
                .paymentStatus("REFUND_PENDING")
                .commissionRate(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("999.00"))
                .build());

        paymentRepository.save(Payment.builder()
                .customer(customerB)
                .order(orderB)
                .amount(new BigDecimal("999.00"))
                .currency("INR")
                .status("CAPTURED")
                .razorpayPaymentId("pay_ownership_123")
                .razorpayOrderId("order_ownership_123")
                .build());

        // Authenticate as User A
        FirebaseUserPrincipal principalA = FirebaseUserPrincipal.create(
                customerA.getCustomerId(), "firebase_uid_a", "user_a@test.com", "User A", Role.CUSTOMER, null, true, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principalA, null, principalA.getAuthorities()));

        try {
            assertThatThrownBy(() -> paymentService.refundOrderPayment(orderB.getOrderId(), "User A attempting refund"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("You can only refund your own orders");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("6. Real MySQL: Order cancel triggers @TransactionalEventListener(AFTER_COMMIT) and executes refund in REQUIRES_NEW")
    void testOrderCancel_TriggersAfterCommitRefund_RealMysql() throws Exception {
        Customer c = createCustomer("9100000007");
        Store s = createStore("store-cancel-event-test");

        Order o = orderRepository.save(Order.builder()
                .customer(c)
                .store(s)
                .orderNumber("ORD-CANCEL-AUTO-001")
                .orderStatus("PLACED")
                .paymentStatus("PAID")
                .commissionRate(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("1200.00"))
                .build());

        Payment p = paymentRepository.save(Payment.builder()
                .customer(c)
                .order(o)
                .amount(new BigDecimal("1200.00"))
                .currency("INR")
                .status("CAPTURED")
                .razorpayPaymentId("pay_cancel_auto_999")
                .razorpayOrderId("order_cancel_auto_999")
                .build());

        Refund mockRefund = mock(Refund.class);
        when(mockRefund.has("id")).thenReturn(true);
        when(mockRefund.get("id")).thenReturn("rfnd_cancel_auto_success");
        when(mockPaymentClient.refund(eq("pay_cancel_auto_999"), any(JSONObject.class))).thenReturn(mockRefund);

        FirebaseUserPrincipal principal = FirebaseUserPrincipal.create(
                c.getCustomerId(), "firebase_uid_c", "user_c@test.com", "User C", Role.CUSTOMER, null, true, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        try {
            // Goes through full OrderServiceImpl.cancelOrder
            orderService.cancelOrder(o.getOrderId(), "Hyderabad", "Customer cancelled before dispatch");

            Order reloadedOrder = orderRepository.findById(o.getOrderId()).orElseThrow();
            assertThat(reloadedOrder.getOrderStatus()).isEqualTo("CANCELLED");

            // Payment must have been processed by the AFTER_COMMIT listener and reached REFUNDED
            Payment reloadedPayment = paymentRepository.findById(p.getPaymentId()).orElseThrow();
            assertThat(reloadedPayment.getStatus()).isEqualTo("REFUNDED");
            verify(mockPaymentClient, atLeastOnce()).refund(eq("pay_cancel_auto_999"), any(JSONObject.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("7. Real MySQL: Order cancel when gateway fails transitions to REFUND_PENDING without unhandled exception")
    void testOrderCancel_WhenGatewayFails_BecomesRefundPending_RealMysql() throws Exception {
        Customer c = createCustomer("9100000008");
        Store s = createStore("store-cancel-fail-test");

        Order o = orderRepository.save(Order.builder()
                .customer(c)
                .store(s)
                .orderNumber("ORD-CANCEL-FAIL-001")
                .orderStatus("PLACED")
                .paymentStatus("PAID")
                .commissionRate(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("1200.00"))
                .build());

        Payment p = paymentRepository.save(Payment.builder()
                .customer(c)
                .order(o)
                .amount(new BigDecimal("1200.00"))
                .currency("INR")
                .status("CAPTURED")
                .razorpayPaymentId("pay_cancel_fail_999")
                .razorpayOrderId("order_cancel_fail_999")
                .build());

        when(mockPaymentClient.refund(eq("pay_cancel_fail_999"), any(JSONObject.class)))
                .thenThrow(new com.razorpay.RazorpayException("Simulated gateway error"));
        when(mockPaymentClient.fetchAllRefunds("pay_cancel_fail_999"))
                .thenReturn(Collections.emptyList());

        FirebaseUserPrincipal principal = FirebaseUserPrincipal.create(
                c.getCustomerId(), "firebase_uid_fail", "user_fail@test.com", "User Fail", Role.CUSTOMER, null, true, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        try {
            // Cancel order through OrderServiceImpl.cancelOrder
            orderService.cancelOrder(o.getOrderId(), "Hyderabad", "Customer cancelled before dispatch");

            Order reloadedOrder = orderRepository.findById(o.getOrderId()).orElseThrow();
            assertThat(reloadedOrder.getOrderStatus()).isEqualTo("CANCELLED");
            assertThat(reloadedOrder.getPaymentStatus()).isEqualTo("REFUND_PENDING");

            Payment reloadedPayment = paymentRepository.findById(p.getPaymentId()).orElseThrow();
            assertThat(reloadedPayment.getStatus()).isEqualTo("REFUND_PENDING");
            assertThat(reloadedPayment.getErrorDescription()).contains("Simulated gateway error");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("8. Sweep: refunds a CANCELLED order with a CAPTURED payment older than the threshold")
    void testSweep_RefundsCancelledOrderWithCapturedPayment_OlderThanThreshold() throws Exception {
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");

        Customer c = createCustomer("9100000010");
        Store s = createStore("store-sweep-1");
        Order o = orderRepository.save(Order.builder()
                .customer(c).store(s).orderNumber("ORD-SWEEP-001").orderStatus("CANCELLED")
                .paymentStatus("PAID").commissionRate(new BigDecimal("5.00")).totalAmount(new BigDecimal("1500.00")).build());

        Payment p = paymentRepository.save(Payment.builder()
                .customer(c).order(o).amount(new BigDecimal("1500.00")).currency("INR").status("CAPTURED")
                .razorpayPaymentId("pay_sweep_001").razorpayOrderId("order_sweep_001").build());

        jdbcTemplate.update("UPDATE payments SET created_at = ? WHERE payment_id = ?",
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(15)), p.getPaymentId());

        Refund mockRefund = mock(Refund.class);
        when(mockRefund.has("id")).thenReturn(true);
        when(mockRefund.get("id")).thenReturn("rfnd_sweep_001_success");
        when(mockPaymentClient.refund(eq("pay_sweep_001"), any(JSONObject.class))).thenReturn(mockRefund);

        int swept = paymentService.sweepCancelledOrderRefunds(10, 50);
        assertThat(swept).isEqualTo(1);

        Payment reloaded = paymentRepository.findById(p.getPaymentId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("REFUNDED");
        verify(mockPaymentClient, times(1)).refund(eq("pay_sweep_001"), any(JSONObject.class));
    }

    @Test
    @DisplayName("9. Sweep: skips payments newer than threshold, and payments already REFUNDED, IN_PROGRESS, or PENDING")
    void testSweep_SkipsNewerPaymentsAndNonCapturedStates() {
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");

        Customer c = createCustomer("9100000011");
        Store s = createStore("store-sweep-2");

        // 1. Newer than threshold (created 2 mins ago, threshold is 10 mins)
        Order o1 = orderRepository.save(Order.builder().customer(c).store(s).orderNumber("ORD-SWEEP-NEW").orderStatus("CANCELLED").paymentStatus("PAID").commissionRate(new BigDecimal("5.00")).totalAmount(new BigDecimal("100.00")).build());
        Payment p1 = paymentRepository.save(Payment.builder().customer(c).order(o1).amount(new BigDecimal("100.00")).currency("INR").status("CAPTURED").razorpayPaymentId("pay_swp_new").razorpayOrderId("ord_swp_new").build());
        jdbcTemplate.update("UPDATE payments SET created_at = ? WHERE payment_id = ?", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(2)), p1.getPaymentId());

        // 2. Already REFUNDED (older than 15 mins)
        Order o2 = orderRepository.save(Order.builder().customer(c).store(s).orderNumber("ORD-SWEEP-RFND").orderStatus("CANCELLED").paymentStatus("REFUNDED").commissionRate(new BigDecimal("5.00")).totalAmount(new BigDecimal("100.00")).build());
        Payment p2 = paymentRepository.save(Payment.builder().customer(c).order(o2).amount(new BigDecimal("100.00")).currency("INR").status("REFUNDED").razorpayPaymentId("pay_swp_rfnd").razorpayOrderId("ord_swp_rfnd").build());
        jdbcTemplate.update("UPDATE payments SET created_at = ? WHERE payment_id = ?", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(15)), p2.getPaymentId());

        // 3. Already REFUND_IN_PROGRESS (older than 15 mins)
        Order o3 = orderRepository.save(Order.builder().customer(c).store(s).orderNumber("ORD-SWEEP-INPROG").orderStatus("CANCELLED").paymentStatus("PAID").commissionRate(new BigDecimal("5.00")).totalAmount(new BigDecimal("100.00")).build());
        Payment p3 = paymentRepository.save(Payment.builder().customer(c).order(o3).amount(new BigDecimal("100.00")).currency("INR").status("REFUND_IN_PROGRESS").razorpayPaymentId("pay_swp_inprog").razorpayOrderId("ord_swp_inprog").build());
        jdbcTemplate.update("UPDATE payments SET created_at = ? WHERE payment_id = ?", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(15)), p3.getPaymentId());

        // 4. Already REFUND_PENDING (older than 15 mins)
        Order o4 = orderRepository.save(Order.builder().customer(c).store(s).orderNumber("ORD-SWEEP-PEND").orderStatus("CANCELLED").paymentStatus("REFUND_PENDING").commissionRate(new BigDecimal("5.00")).totalAmount(new BigDecimal("100.00")).build());
        Payment p4 = paymentRepository.save(Payment.builder().customer(c).order(o4).amount(new BigDecimal("100.00")).currency("INR").status("REFUND_PENDING").razorpayPaymentId("pay_swp_pend").razorpayOrderId("ord_swp_pend").build());
        jdbcTemplate.update("UPDATE payments SET created_at = ? WHERE payment_id = ?", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(15)), p4.getPaymentId());

        int swept = paymentService.sweepCancelledOrderRefunds(10, 50);
        assertThat(swept).isEqualTo(0);
        verifyNoInteractions(mockPaymentClient);
    }

    @Test
    @DisplayName("10. Sweep concurrency: two sweep runs at once call Razorpay mock once per payment")
    void testSweep_ConcurrentRuns_CallsRazorpayMockOnce() throws Exception {
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");

        Customer c = createCustomer("9100000012");
        Store s = createStore("store-sweep-3");
        Order o = orderRepository.save(Order.builder().customer(c).store(s).orderNumber("ORD-SWEEP-CONC").orderStatus("CANCELLED").paymentStatus("PAID").commissionRate(new BigDecimal("5.00")).totalAmount(new BigDecimal("2000.00")).build());
        Payment p = paymentRepository.save(Payment.builder().customer(c).order(o).amount(new BigDecimal("2000.00")).currency("INR").status("CAPTURED").razorpayPaymentId("pay_swp_conc_001").razorpayOrderId("ord_swp_conc_001").build());
        jdbcTemplate.update("UPDATE payments SET created_at = ? WHERE payment_id = ?", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(20)), p.getPaymentId());

        Refund mockRefund = mock(Refund.class);
        when(mockRefund.has("id")).thenReturn(true);
        when(mockRefund.get("id")).thenReturn("rfnd_conc_sweep_success");
        when(mockPaymentClient.refund(eq("pay_swp_conc_001"), any(JSONObject.class))).thenReturn(mockRefund);

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger totalSwept = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    int count = paymentService.sweepCancelledOrderRefunds(10, 50);
                    totalSwept.addAndGet(count);
                } catch (Exception ignored) {}
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(totalSwept.get()).isEqualTo(1);
        verify(mockPaymentClient, times(1)).refund(eq("pay_swp_conc_001"), any(JSONObject.class));
    }

    @Test
    @DisplayName("11. Sweep resilience: one failing payment does not stop others, ends as REFUND_PENDING")
    void testSweep_OneFailingPaymentDoesNotStopOthers() throws Exception {
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");

        Customer c = createCustomer("9100000013");
        Store s = createStore("store-sweep-4");

        // Payment 1: Fails at gateway
        Order o1 = orderRepository.save(Order.builder().customer(c).store(s).orderNumber("ORD-SWP-FAIL1").orderStatus("CANCELLED").paymentStatus("PAID").commissionRate(new BigDecimal("5.00")).totalAmount(new BigDecimal("500.00")).build());
        Payment p1 = paymentRepository.save(Payment.builder().customer(c).order(o1).amount(new BigDecimal("500.00")).currency("INR").status("CAPTURED").razorpayPaymentId("pay_swp_fail_1").razorpayOrderId("ord_swp_fail_1").build());
        jdbcTemplate.update("UPDATE payments SET created_at = ? WHERE payment_id = ?", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(20)), p1.getPaymentId());

        // Payment 2: Succeeds
        Order o2 = orderRepository.save(Order.builder().customer(c).store(s).orderNumber("ORD-SWP-SUCC2").orderStatus("CANCELLED").paymentStatus("PAID").commissionRate(new BigDecimal("5.00")).totalAmount(new BigDecimal("800.00")).build());
        Payment p2 = paymentRepository.save(Payment.builder().customer(c).order(o2).amount(new BigDecimal("800.00")).currency("INR").status("CAPTURED").razorpayPaymentId("pay_swp_succ_2").razorpayOrderId("ord_swp_succ_2").build());
        jdbcTemplate.update("UPDATE payments SET created_at = ? WHERE payment_id = ?", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(15)), p2.getPaymentId());

        when(mockPaymentClient.refund(eq("pay_swp_fail_1"), any(JSONObject.class)))
                .thenThrow(new com.razorpay.RazorpayException("Simulated gateway failure for payment 1"));
        when(mockPaymentClient.fetchAllRefunds("pay_swp_fail_1"))
                .thenReturn(Collections.emptyList());

        Refund mockRefund2 = mock(Refund.class);
        when(mockRefund2.has("id")).thenReturn(true);
        when(mockRefund2.get("id")).thenReturn("rfnd_swp_succ_2");
        when(mockPaymentClient.refund(eq("pay_swp_succ_2"), any(JSONObject.class))).thenReturn(mockRefund2);

        int swept = paymentService.sweepCancelledOrderRefunds(10, 50);
        // Only successful refunds are counted in processedCount
        assertThat(swept).isEqualTo(1);

        Payment reloaded1 = paymentRepository.findById(p1.getPaymentId()).orElseThrow();
        assertThat(reloaded1.getStatus()).isEqualTo("REFUND_PENDING");
        assertThat(reloaded1.getErrorDescription()).contains("Simulated gateway failure");

        Payment reloaded2 = paymentRepository.findById(p2.getPaymentId()).orElseThrow();
        assertThat(reloaded2.getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    @DisplayName("12. Security: Customer cannot retry REFUND_PENDING, but Admin can")
    void testCustomerCannotRetryRefundPending_AdminCan() throws Exception {
        Customer c = createCustomer("9100000014");
        Store s = createStore("store-retry-perm");

        Order o = orderRepository.save(Order.builder().customer(c).store(s).orderNumber("ORD-RETRY-001").orderStatus("CANCELLED").paymentStatus("REFUND_PENDING").commissionRate(new BigDecimal("5.00")).totalAmount(new BigDecimal("1000.00")).build());
        Payment p = paymentRepository.save(Payment.builder().customer(c).order(o).amount(new BigDecimal("1000.00")).currency("INR").status("REFUND_PENDING").errorDescription("Previous gateway timeout").razorpayPaymentId("pay_retry_perm_001").razorpayOrderId("ord_retry_perm_001").build());

        // 1. Authenticate as Customer -> refundOrderPayment must throw ForbiddenException
        FirebaseUserPrincipal customerPrincipal = FirebaseUserPrincipal.create(
                c.getCustomerId(), "fb_uid_cust_retry", "cust_retry@test.com", "Cust Retry", Role.CUSTOMER, null, true, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(customerPrincipal, null, customerPrincipal.getAuthorities()));

        try {
            assertThatThrownBy(() -> paymentService.refundOrderPayment(o.getOrderId(), "Customer retry"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Retrying pending or in-progress refunds is restricted to administrators");
        } finally {
            SecurityContextHolder.clearContext();
        }

        // 2. Authenticate as Admin -> retryPendingRefund must succeed
        FirebaseUserPrincipal adminPrincipal = FirebaseUserPrincipal.create(
                9999, "fb_uid_admin_retry", "admin_retry@test.com", "Admin Retry", Role.ADMIN, null, true, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities()));

        Refund mockAdminRefund = mock(Refund.class);
        when(mockAdminRefund.has("id")).thenReturn(true);
        when(mockAdminRefund.get("id")).thenReturn("rfnd_admin_retry_succ");
        when(mockPaymentClient.fetchAllRefunds("pay_retry_perm_001")).thenReturn(Collections.emptyList());
        when(mockPaymentClient.refund(eq("pay_retry_perm_001"), any(JSONObject.class))).thenReturn(mockAdminRefund);

        try {
            PaymentStatusResponse response = paymentService.retryPendingRefund(p.getPaymentId());
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("REFUNDED");

            Payment reloaded = paymentRepository.findById(p.getPaymentId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo("REFUNDED");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
