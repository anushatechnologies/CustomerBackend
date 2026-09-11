-- ====================================================================
-- HinchExpress trip-service Initial Schema Migration (V1)
-- Microservice Port: 9004 | Database Schema: hinchexpress_trip
-- Core Domain Invariant: One Order = One Trip (UNIQUE constraint on order_id)
-- ====================================================================

CREATE TABLE IF NOT EXISTS trips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_number VARCHAR(64) NOT NULL UNIQUE,
    order_id INT NOT NULL UNIQUE, -- ENFORCES ONE ORDER = ONE TRIP INVARIANT
    customer_id INT NOT NULL,
    store_id INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT ''DISPATCH_PENDING'',

    -- Driver & Vehicle assignment
    driver_id BIGINT NULL,
    vehicle_id BIGINT NULL,
    vehicle_number VARCHAR(32) NULL,
    driver_name VARCHAR(128) NULL,
    driver_phone VARCHAR(32) NULL,

    -- Origin / Store pickup coordinates
    pickup_latitude DOUBLE NOT NULL,
    pickup_longitude DOUBLE NOT NULL,
    pickup_address VARCHAR(512) NULL,

    -- Destination / Customer delivery coordinates
    delivery_address_id INT NULL,
    delivery_latitude DOUBLE NOT NULL,
    delivery_longitude DOUBLE NOT NULL,
    delivery_address VARCHAR(512) NULL,

    -- Cargo weight & routing metrics
    total_weight_kg DECIMAL(10, 3) NOT NULL,
    estimated_distance_km DECIMAL(10, 2) NULL,
    estimated_duration_minutes INT NULL,

    -- Dual OTP Security Verification
    pickup_otp VARCHAR(6) NOT NULL,
    delivery_otp VARCHAR(6) NOT NULL,
    pickup_otp_verified BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_otp_verified BOOLEAN NOT NULL DEFAULT FALSE,
    otp_failed_attempts INT NOT NULL DEFAULT 0,

    -- Audit Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP NULL,
    arrived_at_pickup_at TIMESTAMP NULL,
    picked_up_at TIMESTAMP NULL,
    arrived_at_delivery_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    cancellation_reason VARCHAR(255) NULL,

    INDEX idx_trips_order_id (order_id),
    INDEX idx_trips_driver_id (driver_id),
    INDEX idx_trips_status (status),
    INDEX idx_trips_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS trip_checkpoints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    checkpoint_status VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(256) NULL,
    location_name VARCHAR(256) NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_checkpoint_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    INDEX idx_checkpoints_trip_status (trip_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS delivery_outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT ''PENDING'',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(512) NULL,

    INDEX idx_outbox_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;