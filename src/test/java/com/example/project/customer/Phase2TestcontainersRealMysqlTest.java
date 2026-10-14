package com.example.project.customer;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@EnabledIf("isDockerAvailable")
public class Phase2TestcontainersRealMysqlTest {

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
            .withDatabaseName("hinchmart_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @BeforeAll
    static void setUpDatabase() throws Exception {
        String jdbcUrl = mysql.getJdbcUrl();
        String user = mysql.getUsername();
        String pass = mysql.getPassword();

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, user, pass)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            conn.createStatement().execute("SET FOREIGN_KEY_CHECKS = 0;");
            conn.createStatement().execute("INSERT INTO sellers (seller_id, name, company_name, email, phone) " +
                    "VALUES (1, 'Test Seller', 'Test Company', 'seller@test.com', '9999999999') " +
                    "ON DUPLICATE KEY UPDATE name=name");
            conn.createStatement().execute("INSERT INTO stores (store_id, seller_id, name, slug, status) " +
                    "VALUES (1, 1, 'Test Store', 'test-store-p2', 'ACTIVE') " +
                    "ON DUPLICATE KEY UPDATE name=name");
            conn.createStatement().execute("INSERT INTO products (product_id, title, slug, price, unit, created_at) VALUES " +
                    "(101, 'Product 101', 'p-101', 100.0, 'pcs', NOW()), " +
                    "(201, 'Product 201', 'p-201', 200.0, 'pcs', NOW()), " +
                    "(202, 'Product 202', 'p-202', 200.0, 'pcs', NOW()), " +
                    "(301, 'Product 301', 'p-301', 300.0, 'pcs', NOW()) " +
                    "ON DUPLICATE KEY UPDATE title=title");
            conn.createStatement().execute("SET FOREIGN_KEY_CHECKS = 1;");
        }
    }

    @Test
    @DisplayName("1. Real MySQL: Flyway applies V1 through V27 cleanly on real MySQL 8")
    void testFlywayMigrations_V1toV27_AppliedCleanly() throws Exception {
        String jdbcUrl = mysql.getJdbcUrl();
        String user = mysql.getUsername();
        String pass = mysql.getPassword();

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, user, pass)
                .locations("classpath:db/migration")
                .load();

        // Assert migration history: current version is applied and >= 27
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getState().isApplied()).isTrue();
        int currentVersion = Integer.parseInt(flyway.info().current().getVersion().getVersion().split("\\.")[0]);
        assertThat(currentVersion).isGreaterThanOrEqualTo(27);

        // Verify V27 table hinch_store_vendor_inventory exists
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass);
             ResultSet rs = conn.getMetaData().getTables(null, null, "hinch_store_vendor_inventory", null)) {
            assertThat(rs.next()).isTrue();
        }

        // Verify orders table has fulfillment_store_id and inventory_state
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass);
             ResultSet rs = conn.getMetaData().getColumns(null, null, "orders", "fulfillment_store_id")) {
            assertThat(rs.next()).isTrue();
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass);
             ResultSet rs = conn.getMetaData().getColumns(null, null, "orders", "inventory_state")) {
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    @DisplayName("2. Real MySQL: Concurrent reserve of the last unit with 10 real threads prevents oversell")
    void testConcurrentReserve_LastUnit_NoOversell() throws Exception {
        String jdbcUrl = mysql.getJdbcUrl();
        String user = mysql.getUsername();
        String pass = mysql.getPassword();

        // Seed 1 available unit (qty = 10, reserved_qty = 9)
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            conn.createStatement().execute("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) " +
                    "VALUES (101, 1, 10, 9, TRUE) ON DUPLICATE KEY UPDATE qty=10, reserved_qty=9");
        }

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        String atomicUpdateSql = "UPDATE hinch_store_vendor_inventory " +
                "SET reserved_qty = reserved_qty + 1, updated_at = CURRENT_TIMESTAMP " +
                "WHERE product_id = 101 AND vendor_store_id = 1 AND is_active = TRUE AND (qty - reserved_qty) >= 1";

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass);
                         PreparedStatement stmt = conn.prepareStatement(atomicUpdateSql)) {
                        int rows = stmt.executeUpdate();
                        if (rows > 0) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Exactly 1 thread must have succeeded on the real MySQL database
        assertThat(successCount.get()).isEqualTo(1);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass);
             ResultSet rs = conn.createStatement().executeQuery("SELECT qty, reserved_qty FROM hinch_store_vendor_inventory WHERE product_id = 101 AND vendor_store_id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("qty")).isEqualTo(10);
            assertThat(rs.getInt("reserved_qty")).isEqualTo(10);
        }
    }

    @Test
    @DisplayName("3. Real MySQL: Double commit and double release idempotency")
    void testDoubleCommitAndRelease_Idempotency() throws Exception {
        String jdbcUrl = mysql.getJdbcUrl();
        String user = mysql.getUsername();
        String pass = mysql.getPassword();

        // Seed with qty=5, reserved_qty=2
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            conn.createStatement().execute("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) " +
                    "VALUES (201, 1, 5, 2, TRUE) ON DUPLICATE KEY UPDATE qty=5, reserved_qty=2");
        }

        // Commit: decrement reserved_qty by 2, decrement qty by 2
        String commitSql = "UPDATE hinch_store_vendor_inventory " +
                "SET qty = qty - 2, reserved_qty = reserved_qty - 2, updated_at = CURRENT_TIMESTAMP " +
                "WHERE product_id = 201 AND vendor_store_id = 1 AND is_active = TRUE AND reserved_qty >= 2 AND qty >= 2";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            // First commit -> 1 row updated (qty becomes 3, reserved becomes 0)
            int rows1 = conn.createStatement().executeUpdate(commitSql);
            assertThat(rows1).isEqualTo(1);

            // Second commit attempt -> 0 rows updated (idempotent, no double debit)
            int rows2 = conn.createStatement().executeUpdate(commitSql);
            assertThat(rows2).isEqualTo(0);
        }

        // Release test on product 202 (qty=5, reserved_qty=2)
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            conn.createStatement().execute("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) " +
                    "VALUES (202, 1, 5, 2, TRUE) ON DUPLICATE KEY UPDATE qty=5, reserved_qty=2");
        }

        String releaseSql = "UPDATE hinch_store_vendor_inventory " +
                "SET reserved_qty = reserved_qty - 2, updated_at = CURRENT_TIMESTAMP " +
                "WHERE product_id = 202 AND vendor_store_id = 1 AND is_active = TRUE AND reserved_qty >= 2";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            // First release -> 1 row updated (reserved becomes 0, qty remains 5)
            int rows1 = conn.createStatement().executeUpdate(releaseSql);
            assertThat(rows1).isEqualTo(1);

            // Second release attempt -> 0 rows updated (idempotent, no underflow)
            int rows2 = conn.createStatement().executeUpdate(releaseSql);
            assertThat(rows2).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("4. Real MySQL: Restock after commit increases physical qty back to original")
    void testRestockAfterCommit() throws Exception {
        String jdbcUrl = mysql.getJdbcUrl();
        String user = mysql.getUsername();
        String pass = mysql.getPassword();

        // Seed with qty=10, reserved_qty=0
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            conn.createStatement().execute("INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active) " +
                    "VALUES (301, 1, 10, 0, TRUE) ON DUPLICATE KEY UPDATE qty=10, reserved_qty=0");
        }

        // Simulate order placement (reserve 3) and payment capture (commit 3)
        // Post commit: qty = 7, reserved_qty = 0
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            conn.createStatement().executeUpdate("UPDATE hinch_store_vendor_inventory SET qty = 7, reserved_qty = 0 WHERE product_id = 301 AND vendor_store_id = 1");
        }

        // Cancelling a COMMITTED order triggers atomicRestock: qty = qty + 3
        String restockSql = "UPDATE hinch_store_vendor_inventory " +
                "SET qty = qty + 3, updated_at = CURRENT_TIMESTAMP " +
                "WHERE product_id = 301 AND vendor_store_id = 1 AND is_active = TRUE";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            int rows = conn.createStatement().executeUpdate(restockSql);
            assertThat(rows).isEqualTo(1);

            try (ResultSet rs = conn.createStatement().executeQuery("SELECT qty, reserved_qty FROM hinch_store_vendor_inventory WHERE product_id = 301 AND vendor_store_id = 1")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt("qty")).isEqualTo(10);
                assertThat(rs.getInt("reserved_qty")).isEqualTo(0);
            }
        }
    }
}
