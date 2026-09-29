-- ========================================================
-- Flyway Database Migration: V13__add_seller_order_acceptance_and_rider_dispatch.sql
-- Multi-seller order acceptance, customer delivery OTP, and sequential rider dispatch
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Create delivery_riders table
CREATE TABLE IF NOT EXISTS delivery_riders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type VARCHAR(50) NOT NULL DEFAULT 'TWO_WHEELER',
    vehicle_number VARCHAR(50) NOT NULL,
    current_latitude DOUBLE NULL,
    current_longitude DOUBLE NULL,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    fcm_token VARCHAR(255) NULL,
    active_order_id INT NULL,
    last_location_update DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rider_online_avail (is_online, is_available),
    INDEX idx_rider_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Create rider_dispatch_offers table
CREATE TABLE IF NOT EXISTS rider_dispatch_offers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    rider_id BIGINT NOT NULL,
    sequence_index INT NOT NULL DEFAULT 1,
    distance_km DOUBLE NOT NULL DEFAULT 0.0,
    offered_fare DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'OFFERED',
    offered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    responded_at DATETIME NULL,
    rejection_reason VARCHAR(255) NULL,
    INDEX idx_rdo_order (order_id),
    INDEX idx_rdo_rider (rider_id),
    INDEX idx_rdo_status (status),
    CONSTRAINT fk_rdo_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT fk_rdo_rider FOREIGN KEY (rider_id) REFERENCES delivery_riders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS upgrade_seller_rider_dispatch_v13;

DELIMITER $$

CREATE PROCEDURE upgrade_seller_rider_dispatch_v13()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'seller_accepted_at') THEN
        ALTER TABLE orders ADD COLUMN seller_accepted_at DATETIME NULL AFTER order_status;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'seller_rejection_reason') THEN
        ALTER TABLE orders ADD COLUMN seller_rejection_reason VARCHAR(255) NULL AFTER seller_accepted_at;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'delivery_otp') THEN
        ALTER TABLE orders ADD COLUMN delivery_otp VARCHAR(6) NULL AFTER seller_rejection_reason;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'delivery_otp_generated_at') THEN
        ALTER TABLE orders ADD COLUMN delivery_otp_generated_at DATETIME NULL AFTER delivery_otp;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'delivery_otp_attempts') THEN
        ALTER TABLE orders ADD COLUMN delivery_otp_attempts INT NOT NULL DEFAULT 0 AFTER delivery_otp_generated_at;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'delivery_otp_verified_at') THEN
        ALTER TABLE orders ADD COLUMN delivery_otp_verified_at DATETIME NULL AFTER delivery_otp_attempts;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'rider_id') THEN
        ALTER TABLE orders ADD COLUMN rider_id BIGINT NULL AFTER driver_name;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'rider_phone') THEN
        ALTER TABLE orders ADD COLUMN rider_phone VARCHAR(20) NULL AFTER rider_id;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'rider_assigned_at') THEN
        ALTER TABLE orders ADD COLUMN rider_assigned_at DATETIME NULL AFTER rider_phone;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'picked_up_at') THEN
        ALTER TABLE orders ADD COLUMN picked_up_at DATETIME NULL AFTER rider_assigned_at;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'delivered_at') THEN
        ALTER TABLE orders ADD COLUMN delivered_at DATETIME NULL AFTER picked_up_at;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'cod_amount_collected') THEN
        ALTER TABLE orders ADD COLUMN cod_amount_collected DECIMAL(14, 2) NULL AFTER payment_status;
    END IF;
END $$

DELIMITER ;

CALL upgrade_seller_rider_dispatch_v13();

DROP PROCEDURE IF EXISTS upgrade_seller_rider_dispatch_v13;

SET FOREIGN_KEY_CHECKS = 1;
