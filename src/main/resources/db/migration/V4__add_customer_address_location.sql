-- ========================================================
-- Flyway Migration: V4__add_customer_address_location.sql
-- Adds location coordinates, house/flat, area, country,
-- address type, and timestamp tracking to user_addresses table.
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS user_addresses (
    address_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    site_name VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255) NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    landmark VARCHAR(255) NULL,
    is_default BOOLEAN DEFAULT FALSE,
    has_heavy_vehicle_access BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS upgrade_user_addresses_v4;

DELIMITER $$

CREATE PROCEDURE upgrade_user_addresses_v4()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'user_addresses' AND column_name = 'house_flat_no'
    ) THEN
        ALTER TABLE user_addresses ADD COLUMN house_flat_no VARCHAR(255) NULL AFTER address_line2;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'user_addresses' AND column_name = 'area_locality'
    ) THEN
        ALTER TABLE user_addresses ADD COLUMN area_locality VARCHAR(255) NULL AFTER house_flat_no;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'user_addresses' AND column_name = 'country'
    ) THEN
        ALTER TABLE user_addresses ADD COLUMN country VARCHAR(100) DEFAULT 'India' AFTER state;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'user_addresses' AND column_name = 'latitude'
    ) THEN
        ALTER TABLE user_addresses ADD COLUMN latitude DOUBLE NULL AFTER country;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'user_addresses' AND column_name = 'longitude'
    ) THEN
        ALTER TABLE user_addresses ADD COLUMN longitude DOUBLE NULL AFTER latitude;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'user_addresses' AND column_name = 'address_type'
    ) THEN
        ALTER TABLE user_addresses ADD COLUMN address_type VARCHAR(50) DEFAULT 'OTHER' AFTER longitude;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'user_addresses' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE user_addresses ADD COLUMN updated_at DATETIME NULL AFTER created_at;
    END IF;

    -- Add index on (customer_id, is_default) for fast default address lookups
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE table_schema = DATABASE() AND table_name = 'user_addresses' AND index_name = 'idx_user_addresses_customer_default'
    ) THEN
        ALTER TABLE user_addresses ADD INDEX idx_user_addresses_customer_default (customer_id, is_default);
    END IF;

    -- Add index on customer_id for customer addresses listing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE table_schema = DATABASE() AND table_name = 'user_addresses' AND index_name = 'idx_user_addresses_customer_id'
    ) THEN
        ALTER TABLE user_addresses ADD INDEX idx_user_addresses_customer_id (customer_id);
    END IF;
END $$

DELIMITER ;

CALL upgrade_user_addresses_v4();

DROP PROCEDURE IF EXISTS upgrade_user_addresses_v4;

SET FOREIGN_KEY_CHECKS = 1;
