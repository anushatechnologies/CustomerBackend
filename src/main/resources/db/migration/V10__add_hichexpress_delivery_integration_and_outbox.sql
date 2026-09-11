-- ========================================================
-- Flyway Database Migration: V10__add_hichexpress_delivery_integration_and_outbox.sql
-- HichExpress delivery integration columns and transactional outbox table
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Create outbox_events table for transactional outbox pattern
CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    error_message TEXT NULL,
    INDEX idx_outbox_status_created (status, created_at),
    UNIQUE KEY uk_outbox_aggregate_event (aggregate_type, aggregate_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS upgrade_hichexpress_v10;

DELIMITER $$

CREATE PROCEDURE upgrade_hichexpress_v10()
BEGIN
    -- 2. Add nullable weight_kg to products
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'weight_kg'
    ) THEN
        ALTER TABLE products ADD COLUMN weight_kg DECIMAL(10, 3) NULL AFTER unit;
    END IF;

    -- 3. Add nullable weight_kg to order_items
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'order_items' AND column_name = 'weight_kg'
    ) THEN
        ALTER TABLE order_items ADD COLUMN weight_kg DECIMAL(10, 3) NULL AFTER unit;
    END IF;

    -- 4. Add nullable total_weight_kg to orders
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'total_weight_kg'
    ) THEN
        ALTER TABLE orders ADD COLUMN total_weight_kg DECIMAL(12, 3) NULL AFTER total_amount;
    END IF;

    -- 5. Add nullable latitude to stores
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'stores' AND column_name = 'latitude'
    ) THEN
        ALTER TABLE stores ADD COLUMN latitude DOUBLE NULL AFTER service_radius_km;
    END IF;

    -- 6. Add nullable longitude to stores
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'stores' AND column_name = 'longitude'
    ) THEN
        ALTER TABLE stores ADD COLUMN longitude DOUBLE NULL AFTER latitude;
    END IF;
END $$

DELIMITER ;

CALL upgrade_hichexpress_v10();

DROP PROCEDURE IF EXISTS upgrade_hichexpress_v10;

SET FOREIGN_KEY_CHECKS = 1;
