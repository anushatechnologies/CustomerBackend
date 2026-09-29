-- ========================================================
-- Flyway Database Migration: V14__create_vehicle_types_table_and_order_selected_vehicle.sql
-- Admin Vehicle Type & Fare Management + Seller Vehicle Selection
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Create vehicle_types table
CREATE TABLE IF NOT EXISTS vehicle_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    max_weight_kg DECIMAL(10, 2) NOT NULL DEFAULT 50.00,
    size_dimensions VARCHAR(100) NULL,
    base_fare DECIMAL(10, 2) NOT NULL DEFAULT 40.00,
    base_distance_km DOUBLE NOT NULL DEFAULT 2.0,
    per_km_rate DECIMAL(10, 2) NOT NULL DEFAULT 12.00,
    minimum_fare DECIMAL(10, 2) NOT NULL DEFAULT 40.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    image_url VARCHAR(2000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vt_code (code),
    INDEX idx_vt_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Seed default vehicle types with standard capacity and fares
INSERT IGNORE INTO vehicle_types (id, code, name, description, max_weight_kg, size_dimensions, base_fare, base_distance_km, per_km_rate, minimum_fare, is_active, sort_order)
VALUES 
(1, 'TWO_WHEELER', '2-Wheeler (Bike / Scooter)', 'Ideal for small packages, hardware items, fasteners, and paints up to 20kg', 20.00, '1 x 1 x 1 ft', 40.00, 2.0, 10.00, 40.00, TRUE, 1),
(2, 'THREE_WHEELER', '3-Wheeler Auto / Electric Loader', 'Suitable for medium loads, tiles, electrical coils, and plumbing supplies up to 500kg', 500.00, '4.5 x 4 x 3.5 ft (Open)', 150.00, 3.0, 18.00, 150.00, TRUE, 2),
(3, 'TATA_ACE', 'Tata Ace / Chota Hathi (1.5 Ton)', 'Standard mini-truck for cement bags, light sanitary ware, and steel bundles up to 1000kg', 1000.00, '7 x 4.8 x 4.8 ft', 250.00, 4.0, 25.00, 250.00, TRUE, 3),
(4, 'PICKUP_8FT', '8ft Bolero / Dost Pickup', 'Heavy pickup truck for full pallets, bricks, pipes, and medium rebars up to 1750kg', 1750.00, '8.2 x 5 x 5 ft', 400.00, 5.0, 32.00, 400.00, TRUE, 4),
(5, 'TATA_407', 'Tata 407 (3.5 Ton Freight)', 'Heavy industrial freight truck for commercial structural steel, bulk cement, and large machinery', 3500.00, '10 x 6 x 6 ft', 750.00, 5.0, 45.00, 750.00, TRUE, 5);

-- 3. Add selected_vehicle_type column to orders table
DROP PROCEDURE IF EXISTS upgrade_vehicle_selection_v14;

DELIMITER $$

CREATE PROCEDURE upgrade_vehicle_selection_v14()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'selected_vehicle_type'
    ) THEN
        ALTER TABLE orders ADD COLUMN selected_vehicle_type VARCHAR(50) NULL AFTER delivery_otp;
    END IF;
END $$

DELIMITER ;

CALL upgrade_vehicle_selection_v14();

DROP PROCEDURE IF EXISTS upgrade_vehicle_selection_v14;

SET FOREIGN_KEY_CHECKS = 1;
