-- ========================================================
-- Fleet Service Schema: V1__init_fleet_tables.sql
-- ========================================================

CREATE TABLE IF NOT EXISTS vehicle_tiers (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    max_payload_kg DECIMAL(12, 3) NOT NULL,
    volumetric_capacity_m3 DECIMAL(10, 2) NULL,
    base_fare DECIMAL(10, 2) NOT NULL DEFAULT 150.00,
    per_km_rate DECIMAL(10, 2) NOT NULL DEFAULT 25.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS drivers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) NULL,
    license_number VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    rating DECIMAL(3, 2) NOT NULL DEFAULT 4.80,
    total_trips_completed INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_drivers_status (status, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_number VARCHAR(30) NOT NULL UNIQUE,
    tier_code VARCHAR(50) NOT NULL,
    driver_id BIGINT NULL,
    model_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    current_latitude DOUBLE NULL,
    current_longitude DOUBLE NULL,
    last_ping_at DATETIME NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY fk_vehicles_tier (tier_code) REFERENCES vehicle_tiers(code),
    FOREIGN KEY fk_vehicles_driver (driver_id) REFERENCES drivers(id),
    INDEX idx_vehicles_tier_status (tier_code, status, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
