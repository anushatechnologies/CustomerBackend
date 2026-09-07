-- ========================================================
-- Flyway Migration: V2__align_live_db_to_code.sql
-- Safely aligns live database schema with Customer/Seller architecture.
-- 100% Data-preserving and Idempotent (checks information_schema).
-- Zero changes to products, categories, subcategories, brands, banners.
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS align_schema_v2;

DELIMITER $$

CREATE PROCEDURE align_schema_v2()
BEGIN
    -- ----------------------------------------------------
    -- 1. Migrate vendor columns to seller in purchase_orders
    -- ----------------------------------------------------
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'purchase_orders' AND column_name = 'vendor_id'
    ) THEN
        ALTER TABLE purchase_orders CHANGE COLUMN vendor_id seller_id INT NOT NULL;
    END IF;

    -- ----------------------------------------------------
    -- 2. Migrate vendor columns to seller in rfq_quotations
    -- ----------------------------------------------------
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'rfq_quotations' AND column_name = 'vendor_id'
    ) THEN
        ALTER TABLE rfq_quotations CHANGE COLUMN vendor_id seller_id INT NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'rfq_quotations' AND column_name = 'vendor_name'
    ) THEN
        ALTER TABLE rfq_quotations CHANGE COLUMN vendor_name seller_name VARCHAR(255) NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'rfq_quotations' AND column_name = 'vendor_rating'
    ) THEN
        ALTER TABLE rfq_quotations CHANGE COLUMN vendor_rating seller_rating DOUBLE DEFAULT 4.8;
    END IF;

    -- ----------------------------------------------------
    -- 3. Migrate user_id to customer_id across all 9 tables
    -- ----------------------------------------------------
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'purchase_orders' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE purchase_orders CHANGE COLUMN user_id customer_id INT NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'carts' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE carts CHANGE COLUMN user_id customer_id INT;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE orders CHANGE COLUMN user_id customer_id INT;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'rental_bookings' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE rental_bookings CHANGE COLUMN user_id customer_id INT NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'reward_vouchers' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE reward_vouchers CHANGE COLUMN user_id customer_id INT;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'rfqs' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE rfqs CHANGE COLUMN user_id customer_id INT;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'support_tickets' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE support_tickets CHANGE COLUMN user_id customer_id INT NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'wallets' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE wallets CHANGE COLUMN user_id customer_id INT NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'wishlist_items' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE wishlist_items CHANGE COLUMN user_id customer_id INT;
    END IF;

    -- ----------------------------------------------------
    -- 4. Add required columns to customers table
    -- ----------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'customers' AND column_name = 'role'
    ) THEN
        ALTER TABLE customers ADD COLUMN role VARCHAR(255) NOT NULL DEFAULT 'BUYER';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'customers' AND column_name = 'password_hash'
    ) THEN
        ALTER TABLE customers ADD COLUMN password_hash VARCHAR(255) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'customers' AND column_name = 'is_active'
    ) THEN
        ALTER TABLE customers ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'customers' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE customers ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'customers' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE customers ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
    END IF;

    -- ----------------------------------------------------
    -- 5. If user_profiles has password_hash, sync to customers
    -- ----------------------------------------------------
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'password_hash'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'user_profiles' AND column_name = 'user_id'
    ) THEN
        UPDATE customers c
        JOIN user_profiles up ON c.customer_id = up.user_id
        SET c.password_hash = up.password_hash
        WHERE c.password_hash IS NULL AND up.password_hash IS NOT NULL;
    END IF;

END $$

DELIMITER ;

CALL align_schema_v2();

DROP PROCEDURE IF EXISTS align_schema_v2;

SET FOREIGN_KEY_CHECKS = 1;
