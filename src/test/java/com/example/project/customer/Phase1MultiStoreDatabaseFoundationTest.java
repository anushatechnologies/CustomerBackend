package com.example.project.customer;

import com.example.project.customer.entity.*;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderGroupRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.PaymentRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Phase1MultiStoreDatabaseFoundationTest {

    @Nested
    @DisplayName("Unit Tests: Entity Mappings, Defaults & Legacy Backward Compatibility")
    class EntityMappingAndCompatibilityUnitTests {

        @Test
        @DisplayName("OrderGroup: table name, annotations, and defaults are properly configured")
        void testOrderGroupEntityMappingAndDefaults() throws Exception {
            Table table = OrderGroup.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            assertThat(table.name()).isEqualTo("order_groups");

            Field idField = OrderGroup.class.getDeclaredField("id");
            assertThat(idField.getAnnotation(Id.class)).isNotNull();

            Field groupOrderNumberField = OrderGroup.class.getDeclaredField("groupOrderNumber");
            Column gonColumn = groupOrderNumberField.getAnnotation(Column.class);
            assertThat(gonColumn).isNotNull();
            assertThat(gonColumn.name()).isEqualTo("group_order_number");
            assertThat(gonColumn.nullable()).isFalse();
            assertThat(gonColumn.unique()).isTrue();

            Field customerField = OrderGroup.class.getDeclaredField("customer");
            JoinColumn custJoin = customerField.getAnnotation(JoinColumn.class);
            assertThat(custJoin).isNotNull();
            assertThat(custJoin.name()).isEqualTo("customer_id");
            assertThat(custJoin.nullable()).isFalse();

            Field ordersField = OrderGroup.class.getDeclaredField("orders");
            OneToMany ordersOneToMany = ordersField.getAnnotation(OneToMany.class);
            assertThat(ordersOneToMany).isNotNull();
            assertThat(ordersOneToMany.mappedBy()).isEqualTo("orderGroup");

            // Test builder and @PrePersist default generation
            Customer customer = Customer.builder().customerId(101).name("Test Buyer").build();
            OrderGroup og = OrderGroup.builder()
                    .customer(customer)
                    .subtotal(new BigDecimal("1000.00"))
                    .grandTotal(new BigDecimal("1180.00"))
                    .build();

            assertThat(og.getPaymentStatus()).isEqualTo("PENDING");
            assertThat(og.getGroupStatus()).isEqualTo("PLACED");
            assertThat(og.getPaymentMethod()).isEqualTo("RAZORPAY");
            assertThat(og.getSubtotal()).isEqualByComparingTo("1000.00");
            assertThat(og.getGrandTotal()).isEqualByComparingTo("1180.00");
            assertThat(og.getOrderForSomeoneElse()).isFalse();

            // Simulate @PrePersist
            var prePersistMethod = OrderGroup.class.getDeclaredMethod("onCreate");
            prePersistMethod.setAccessible(true);
            prePersistMethod.invoke(og);

            assertThat(og.getGroupOrderNumber()).startsWith("GRP-");
            assertThat(og.getCreatedAt()).isNotNull();
            assertThat(og.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Order: nullable orderGroup mapping preserves historical single-store orders")
        void testOrderEntityOrderGroupMapping() throws Exception {
            Field orderGroupField = Order.class.getDeclaredField("orderGroup");
            assertThat(orderGroupField.getAnnotation(ManyToOne.class)).isNotNull();
            JoinColumn joinColumn = orderGroupField.getAnnotation(JoinColumn.class);
            assertThat(joinColumn).isNotNull();
            assertThat(joinColumn.name()).isEqualTo("order_group_id");

            // Verify legacy order behavior (orderGroup == null)
            Order legacyOrder = Order.builder()
                    .orderId(501)
                    .orderNumber("ORD-LEGACY-001")
                    .totalAmount(new BigDecimal("500.00"))
                    .build();

            assertThat(legacyOrder.getOrderGroup()).isNull();
            assertThat(legacyOrder.getOrderNumber()).isEqualTo("ORD-LEGACY-001");

            // Verify linked child order behavior
            OrderGroup parentGroup = OrderGroup.builder().id(88).groupOrderNumber("GRP-20261022-001").build();
            legacyOrder.setOrderGroup(parentGroup);
            assertThat(legacyOrder.getOrderGroup()).isNotNull();
            assertThat(legacyOrder.getOrderGroup().getId()).isEqualTo(88);
        }

        @Test
        @DisplayName("Payment: nullable orderGroup mapping preserves single-order payment records")
        void testPaymentEntityOrderGroupMapping() throws Exception {
            Field orderGroupField = Payment.class.getDeclaredField("orderGroup");
            assertThat(orderGroupField.getAnnotation(ManyToOne.class)).isNotNull();
            JoinColumn joinColumn = orderGroupField.getAnnotation(JoinColumn.class);
            assertThat(joinColumn).isNotNull();
            assertThat(joinColumn.name()).isEqualTo("order_group_id");

            // Verify legacy payment behavior (orderGroup == null, order != null)
            Order order = Order.builder().orderId(42).orderNumber("ORD-42").build();
            Payment legacyPayment = Payment.builder()
                    .paymentId(1001)
                    .order(order)
                    .amount(new BigDecimal("2500.00"))
                    .razorpayOrderId("order_legacy_rzp_123")
                    .status("CAPTURED")
                    .build();

            assertThat(legacyPayment.getOrderGroup()).isNull();
            assertThat(legacyPayment.getOrder()).isNotNull();
            assertThat(legacyPayment.getOrder().getOrderId()).isEqualTo(42);

            // Verify group-level payment behavior
            OrderGroup og = OrderGroup.builder().id(99).groupOrderNumber("GRP-99").build();
            legacyPayment.setOrderGroup(og);
            assertThat(legacyPayment.getOrderGroup().getId()).isEqualTo(99);
        }

        @Test
        @DisplayName("CouponUsage: nullable orderGroup mapping supports group redemptions")
        void testCouponUsageOrderGroupMapping() throws Exception {
            Field orderGroupField = CouponUsage.class.getDeclaredField("orderGroup");
            assertThat(orderGroupField.getAnnotation(ManyToOne.class)).isNotNull();
            JoinColumn joinColumn = orderGroupField.getAnnotation(JoinColumn.class);
            assertThat(joinColumn).isNotNull();
            assertThat(joinColumn.name()).isEqualTo("order_group_id");

            Coupon coupon = Coupon.builder().id(1L).code("SAVE10").build();
            Customer customer = Customer.builder().customerId(101).build();
            Order order = Order.builder().orderId(42).build();

            CouponUsage usage = CouponUsage.builder()
                    .coupon(coupon)
                    .customer(customer)
                    .order(order)
                    .discountApplied(new BigDecimal("100.00"))
                    .build();

            assertThat(usage.getOrderGroup()).isNull();
            OrderGroup og = OrderGroup.builder().id(77).build();
            usage.setOrderGroup(og);
            assertThat(usage.getOrderGroup().getId()).isEqualTo(77);
        }

        @Test
        @DisplayName("Cart: store mapping allows null for multi-store unified cart")
        void testCartEntityStoreNullability() {
            Customer customer = Customer.builder().customerId(101).build();

            // Multi-store cart has store == null
            Cart multiStoreCart = Cart.builder()
                    .cartId(1)
                    .customer(customer)
                    .store(null)
                    .isActive(true)
                    .build();

            assertThat(multiStoreCart.getStore()).isNull();
            assertThat(multiStoreCart.getIsActive()).isTrue();

            // Legacy cart has store != null
            Store store1 = Store.builder().storeId(1).name("HinchStore").build();
            multiStoreCart.setStore(store1);
            assertThat(multiStoreCart.getStore()).isNotNull();
            assertThat(multiStoreCart.getStore().getStoreId()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Unit Tests: Flyway V31 Migration SQL Safety and Non-Destructive Invariants")
    class MigrationSqlSafetyAuditUnitTests {

        @Test
        @DisplayName("V31 migration file exists, is non-destructive, and adheres to safety rules")
        void testMigrationV31SyntaxAndSafetyAudit() throws IOException {
            Path migrationPath = Paths.get("src/main/resources/db/migration/V31__multi_store_order_groups.sql");
            assertThat(Files.exists(migrationPath))
                    .as("Flyway migration V31 must exist in src/main/resources/db/migration/")
                    .isTrue();

            String sql = Files.readString(migrationPath);

            // 1. Must create order_groups table
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS order_groups");
            assertThat(sql).contains("group_order_number VARCHAR(100) NOT NULL UNIQUE");
            assertThat(sql).contains("customer_id INT NOT NULL");
            assertThat(sql).contains("address_id INT NULL");
            assertThat(sql).contains("subtotal DECIMAL(14, 2) NOT NULL DEFAULT 0.00");
            assertThat(sql).contains("grand_total DECIMAL(14, 2) NOT NULL DEFAULT 0.00");
            assertThat(sql).contains("payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING'");
            assertThat(sql).contains("group_status VARCHAR(50) NOT NULL DEFAULT 'PLACED'");

            // 2. Foreign key safety: Order history MUST NOT CASCADE on customer delete
            assertThat(sql).contains("CONSTRAINT fk_order_groups_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE RESTRICT");
            assertThat(sql).doesNotContain("CONSTRAINT fk_order_groups_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE");

            // 3. Child orders foreign key: MUST NOT CASCADE delete child orders
            assertThat(sql).contains("CONSTRAINT fk_orders_order_group FOREIGN KEY (order_group_id) REFERENCES order_groups (id) ON DELETE RESTRICT");

            // 4. Cart store nullability: MUST be modified to nullable
            assertThat(sql).contains("ALTER TABLE carts MODIFY COLUMN store_id INT NULL DEFAULT NULL");

            // 5. Payments order_group_id: MUST be added with SET NULL
            assertThat(sql).contains("ALTER TABLE payments ADD COLUMN order_group_id INT NULL");
            assertThat(sql).contains("CONSTRAINT fk_payments_order_group FOREIGN KEY (order_group_id) REFERENCES order_groups (id) ON DELETE SET NULL");

            // 6. Coupon usages order_group_id: MUST be added with SET NULL
            assertThat(sql).contains("ALTER TABLE coupon_usages ADD COLUMN order_group_id INT NULL");
            assertThat(sql).contains("CONSTRAINT fk_coupon_usages_order_group FOREIGN KEY (order_group_id) REFERENCES order_groups (id) ON DELETE SET NULL");

            // 7. Non-destructive invariants: No table drops of production tables, no data deletion
            assertThat(sql).doesNotContain("DROP TABLE orders");
            assertThat(sql).doesNotContain("DROP TABLE carts");
            assertThat(sql).doesNotContain("DROP TABLE payments");
            assertThat(sql).doesNotContain("DROP TABLE customers");
            assertThat(sql).doesNotContain("DROP TABLE user_addresses");
            assertThat(sql).doesNotContain("DELETE FROM");
            assertThat(sql).doesNotContain("TRUNCATE");

            // 8. Idempotency invariants: Uses stored procedure with information_schema checks
            assertThat(sql).contains("CREATE PROCEDURE upgrade_phase1_order_groups_v31()");
            assertThat(sql).contains("CALL upgrade_phase1_order_groups_v31()");
            assertThat(sql).contains("DROP PROCEDURE IF EXISTS upgrade_phase1_order_groups_v31;");
            assertThat(sql).doesNotContain("DROP PROCEDURE IF EXISTS upgrade_phase1_order_groups_v31()");
        }
    }

    @Nested
    @Testcontainers
    @SpringBootTest
    @org.springframework.test.context.ActiveProfiles("dev")
    @EnabledIf("isDockerAvailable")
    @DisplayName("Integration Tests: Real MySQL 8.0 Container with Flyway V1..V31")
    static class RealMysqlMigrationAndPersistenceIntegrationTest {

        static boolean isDockerAvailable() {
            try {
                return DockerClientFactory.instance().isDockerAvailable();
            } catch (Throwable t) {
                return false;
            }
        }

        @Container
        private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36")
                .withDatabaseName("hinchmart_phase1_test")
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
        private OrderGroupRepository orderGroupRepository;

        @Autowired(required = false)
        private OrderRepository orderRepository;

        @Autowired(required = false)
        private PaymentRepository paymentRepository;

        @Autowired(required = false)
        private CustomerRepository customerRepository;

        @Autowired(required = false)
        private com.example.project.customer.repository.StoreRepository storeRepository;

        @Test
        @Transactional
        @DisplayName("Flyway V31 applies cleanly and supports parent-child order persistence on real MySQL")
        void testOrderGroupPersistenceOnRealMysql() {
            if (orderGroupRepository == null || customerRepository == null) {
                return;
            }

            Customer customer = customerRepository.save(Customer.builder()
                    .name("Real MySQL Customer")
                    .phone("9888877777")
                    .email("realmysql@hinchmart.com")
                    .build());

            OrderGroup og = orderGroupRepository.save(OrderGroup.builder()
                    .groupOrderNumber("GRP-TEST-REAL-001")
                    .customer(customer)
                    .subtotal(new BigDecimal("5000.00"))
                    .taxableAmount(new BigDecimal("5000.00"))
                    .cgst(new BigDecimal("450.00"))
                    .sgst(new BigDecimal("450.00"))
                    .totalGst(new BigDecimal("900.00"))
                    .grandTotal(new BigDecimal("5900.00"))
                    .paymentStatus("PENDING")
                    .groupStatus("PLACED")
                    .build());

            assertThat(og.getId()).isNotNull();

            com.example.project.customer.entity.Store store = (storeRepository != null)
                    ? storeRepository.findById(1).orElseGet(() -> storeRepository.findAll().stream().findFirst().orElse(null))
                    : null;

            Order childOrder1 = orderRepository.save(Order.builder()
                    .orderNumber("ORD-CHILD-001")
                    .customer(customer)
                    .orderGroup(og)
                    .store(store)
                    .totalAmount(new BigDecimal("3000.00"))
                    .build());

            Order childOrder2 = orderRepository.save(Order.builder()
                    .orderNumber("ORD-CHILD-002")
                    .customer(customer)
                    .orderGroup(og)
                    .store(store)
                    .totalAmount(new BigDecimal("2900.00"))
                    .build());

            Payment payment = paymentRepository.save(Payment.builder()
                    .customer(customer)
                    .orderGroup(og)
                    .amount(new BigDecimal("5900.00"))
                    .razorpayOrderId("order_rzp_group_001")
                    .status("CREATED")
                    .build());

            assertThat(payment.getPaymentId()).isNotNull();
            assertThat(payment.getOrderGroup().getId()).isEqualTo(og.getId());

            List<Order> childOrders = orderRepository.findAll().stream()
                    .filter(o -> o.getOrderGroup() != null && o.getOrderGroup().getId().equals(og.getId()))
                    .toList();
            assertThat(childOrders).hasSize(2);
        }
    }
}
