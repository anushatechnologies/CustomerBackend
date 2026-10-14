package com.example.project.customer;

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

import static org.assertj.core.api.Assertions.assertThat;
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
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
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
}
