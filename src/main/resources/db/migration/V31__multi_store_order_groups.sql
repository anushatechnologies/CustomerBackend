-- ========================================================
-- Flyway Database Migration: V31__multi_store_order_groups.sql
-- Phase 1: Database Foundation for Unified Multi-Store Order Groups
-- Safe, 100% data-preserving, backward-compatible, non-destructive
-- ========================================================

-- 1. Create order_groups parent table
CREATE TABLE IF NOT EXISTS order_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    group_order_number VARCHAR(100) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    address_id INT NULL,
    delivery_location TEXT NULL,
    order_for_someone_else BOOLEAN DEFAULT FALSE,
    recipient_name VARCHAR(150) NULL,
    recipient_phone VARCHAR(50) NULL,
    subtotal DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    total_discount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    coupon_code VARCHAR(100) NULL,
    taxable_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    cgst DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    sgst DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    igst DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    total_gst DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    total_freight DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    total_crane_charge DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    grand_total DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'RAZORPAY',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    group_status VARCHAR(50) NOT NULL DEFAULT 'PLACED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_groups_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_groups_address FOREIGN KEY (address_id) REFERENCES user_addresses (address_id) ON DELETE SET NULL,
    INDEX idx_order_groups_customer (customer_id),
    INDEX idx_order_groups_payment_status (payment_status),
    INDEX idx_order_groups_group_status (group_status),
    INDEX idx_order_groups_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Procedure for idempotent alteration of carts, orders, payments, coupon_usages
DROP PROCEDURE IF EXISTS upgrade_phase1_order_groups_v31;

DELIMITER $$

CREATE PROCEDURE upgrade_phase1_order_groups_v31()
BEGIN
    -- 2a. Make carts.store_id nullable (allows multi-store unified cart)
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'carts' AND column_name = 'store_id' AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE carts MODIFY COLUMN store_id INT NULL DEFAULT NULL;
    END IF;

    -- 2b. Add order_group_id to orders
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'order_group_id'
    ) THEN
        ALTER TABLE orders ADD COLUMN order_group_id INT NULL AFTER order_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND constraint_name = 'fk_orders_order_group'
    ) THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_order_group FOREIGN KEY (order_group_id) REFERENCES order_groups (id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'idx_orders_order_group_id'
    ) THEN
        ALTER TABLE orders ADD INDEX idx_orders_order_group_id (order_group_id);
    END IF;

    -- 2c. Add order_group_id to payments
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'payments' AND column_name = 'order_group_id'
    ) THEN
        ALTER TABLE payments ADD COLUMN order_group_id INT NULL AFTER order_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE table_schema = DATABASE() AND table_name = 'payments' AND constraint_name = 'fk_payments_order_group'
    ) THEN
        ALTER TABLE payments ADD CONSTRAINT fk_payments_order_group FOREIGN KEY (order_group_id) REFERENCES order_groups (id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE table_schema = DATABASE() AND table_name = 'payments' AND index_name = 'idx_payments_order_group_id'
    ) THEN
        ALTER TABLE payments ADD INDEX idx_payments_order_group_id (order_group_id);
    END IF;

    -- 2d. Add order_group_id to coupon_usages
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'coupon_usages' AND column_name = 'order_group_id'
    ) THEN
        ALTER TABLE coupon_usages ADD COLUMN order_group_id INT NULL AFTER order_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE table_schema = DATABASE() AND table_name = 'coupon_usages' AND constraint_name = 'fk_coupon_usages_order_group'
    ) THEN
        ALTER TABLE coupon_usages ADD CONSTRAINT fk_coupon_usages_order_group FOREIGN KEY (order_group_id) REFERENCES order_groups (id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE table_schema = DATABASE() AND table_name = 'coupon_usages' AND index_name = 'idx_coupon_usages_order_group_id'
    ) THEN
        ALTER TABLE coupon_usages ADD INDEX idx_coupon_usages_order_group_id (order_group_id);
    END IF;

END $$

DELIMITER ;

CALL upgrade_phase1_order_groups_v31();

DROP PROCEDURE IF EXISTS upgrade_phase1_order_groups_v31;
