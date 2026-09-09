-- ========================================================
-- Flyway Migration: V7__multi_seller_marketplace_schema.sql
-- Multi-Seller Marketplace Pivot (Zomato/Swiggy Model)
-- 100% Data-preserving, safe, and idempotent.
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Ensure default platform seller exists for HinchStore
INSERT IGNORE INTO sellers (seller_id, name, email, phone, company_name, business_type, gstin, onboarding_status, verification_status, created_at, updated_at)
VALUES (1, 'HinchMart Admin', 'admin@hinchmart.com', '9999999999', 'HinchMart B2B Commerce Pvt Ltd', 'PRIVATE_LIMITED', '36AAACH2026Q1Z1', 'COMPLETED', 'VERIFIED', NOW(), NOW());

-- 2. Create stores table
CREATE TABLE IF NOT EXISTS stores (
    store_id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    logo_url VARCHAR(2000),
    banner_url VARCHAR(2000),
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    min_order_value DECIMAL(14, 2) DEFAULT 0.00,
    service_radius_km INT DEFAULT 50,
    commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 5.00,
    rating DOUBLE DEFAULT 4.8,
    review_count INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stores_seller (seller_id),
    INDEX idx_stores_slug (slug),
    INDEX idx_stores_status (status),
    CONSTRAINT fk_stores_seller FOREIGN KEY (seller_id) REFERENCES sellers (seller_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Create store_invoice_sequences table (consecutive per-store GST invoice counters)
CREATE TABLE IF NOT EXISTS store_invoice_sequences (
    id INT AUTO_INCREMENT PRIMARY KEY,
    store_id INT NOT NULL,
    financial_year VARCHAR(10) NOT NULL,
    last_sequence_number BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_fy (store_id, financial_year),
    CONSTRAINT fk_invoice_seq_store FOREIGN KEY (store_id) REFERENCES stores (store_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Create category_requests table
CREATE TABLE IF NOT EXISTS category_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    parent_category_id INT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cat_req_seller (seller_id),
    INDEX idx_cat_req_status (status),
    CONSTRAINT fk_cat_req_seller FOREIGN KEY (seller_id) REFERENCES sellers (seller_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Create seller_payout_ledgers table (financial reconciliation with 1% TCS & clawback tracking)
CREATE TABLE IF NOT EXISTS seller_payout_ledgers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    store_id INT NOT NULL,
    order_id INT NULL,
    gross_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    commission_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    tcs_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    net_payout_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    clawback_amount DECIMAL(14, 2) NULL,
    clawback_reason VARCHAR(500) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    settlement_date DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ledger_store (store_id),
    INDEX idx_ledger_order (order_id),
    INDEX idx_ledger_status (status),
    CONSTRAINT fk_ledger_store FOREIGN KEY (store_id) REFERENCES stores (store_id) ON DELETE CASCADE,
    CONSTRAINT fk_ledger_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Seed Store #1: HinchStore (Internal Platform Store)
INSERT IGNORE INTO stores (store_id, seller_id, name, slug, description, status, commission_rate, rating, review_count, created_at, updated_at)
VALUES (1, 1, 'HinchStore', 'hinchstore', 'Official HinchMart Direct Wholesale Construction Supply Store', 'ACTIVE', 5.00, 4.9, 125, NOW(), NOW());

DROP PROCEDURE IF EXISTS upgrade_marketplace_v7;

DELIMITER $$

CREATE PROCEDURE upgrade_marketplace_v7()
BEGIN
    -- 7. Products: store_id & constraints
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'store_id'
    ) THEN
        ALTER TABLE products ADD COLUMN store_id INT NOT NULL DEFAULT 1;
    END IF;

    UPDATE products SET store_id = 1, approval_status = 'APPROVED' WHERE store_id IS NULL OR store_id = 0;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE table_schema = DATABASE() AND table_name = 'products' AND constraint_name = 'fk_products_store'
    ) THEN
        ALTER TABLE products ADD CONSTRAINT fk_products_store FOREIGN KEY (store_id) REFERENCES stores (store_id);
    END IF;

    -- 8. Carts: store_id, is_active & constraints
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'carts' AND column_name = 'store_id'
    ) THEN
        ALTER TABLE carts ADD COLUMN store_id INT NOT NULL DEFAULT 1;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'carts' AND column_name = 'is_active'
    ) THEN
        ALTER TABLE carts ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE table_schema = DATABASE() AND table_name = 'carts' AND constraint_name = 'fk_carts_store'
    ) THEN
        ALTER TABLE carts ADD CONSTRAINT fk_carts_store FOREIGN KEY (store_id) REFERENCES stores (store_id);
    END IF;

    -- 9. Orders: store_id, store_invoice_number, commission_rate, commission_amount & constraints
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'store_id'
    ) THEN
        ALTER TABLE orders ADD COLUMN store_id INT NOT NULL DEFAULT 1;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'store_invoice_number'
    ) THEN
        ALTER TABLE orders ADD COLUMN store_invoice_number VARCHAR(100) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'commission_rate'
    ) THEN
        ALTER TABLE orders ADD COLUMN commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 5.00;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'commission_amount'
    ) THEN
        ALTER TABLE orders ADD COLUMN commission_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND constraint_name = 'fk_orders_store'
    ) THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES stores (store_id);
    END IF;

    -- Backfill existing orders with default invoice format
    UPDATE orders SET store_id = 1 WHERE store_id IS NULL OR store_id = 0;
    UPDATE orders SET store_invoice_number = CONCAT('HM/HNCH/24-25/', LPAD(order_id, 5, '0')) WHERE store_invoice_number IS NULL;
END $$

DELIMITER ;

CALL upgrade_marketplace_v7();

DROP PROCEDURE IF EXISTS upgrade_marketplace_v7;

SET FOREIGN_KEY_CHECKS = 1;
