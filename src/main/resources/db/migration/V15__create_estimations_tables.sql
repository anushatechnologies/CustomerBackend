-- ========================================================
-- Flyway Database Migration: V15__create_estimations_tables.sql
-- Creates Estimations and Estimation Items for AI Requirement Estimation & Quotations
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Estimations Table
CREATE TABLE IF NOT EXISTS estimations (
    estimation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estimation_number VARCHAR(50) NOT NULL,
    customer_id INT NOT NULL,
    original_file_name VARCHAR(255) NULL,
    original_file_type VARCHAR(50) NULL,
    original_file_size BIGINT NULL,
    original_file_url TEXT NULL,
    original_file_key VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    failure_reason TEXT NULL,
    quotation_pdf_url TEXT NULL,
    quotation_pdf_key VARCHAR(255) NULL,
    subtotal DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    grand_total DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    notes TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    CONSTRAINT uk_estimation_number UNIQUE (estimation_number),
    INDEX idx_estimations_customer_id (customer_id),
    INDEX idx_estimations_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Estimation Items Table
CREATE TABLE IF NOT EXISTS estimation_items (
    item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estimation_id BIGINT NOT NULL,
    raw_item_name VARCHAR(255) NOT NULL,
    requested_quantity DECIMAL(12,2) NULL,
    requested_unit VARCHAR(50) NULL,
    requested_brand VARCHAR(100) NULL,
    dimensions VARCHAR(100) NULL,
    specifications TEXT NULL,
    notes TEXT NULL,
    match_status VARCHAR(30) NOT NULL DEFAULT 'NOT_FOUND',
    matched_product_id INT NULL,
    candidate_product_ids TEXT NULL,
    unit_price DECIMAL(12,2) NULL,
    applied_tier_description VARCHAR(200) NULL,
    gst_rate DECIMAL(5,2) NULL,
    line_subtotal DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    line_tax DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    is_available BOOLEAN NOT NULL DEFAULT FALSE,
    available_stock INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_est_items_estimation FOREIGN KEY (estimation_id) REFERENCES estimations (estimation_id) ON DELETE CASCADE,
    CONSTRAINT fk_est_items_product FOREIGN KEY (matched_product_id) REFERENCES products (product_id) ON DELETE SET NULL,
    INDEX idx_est_items_estimation_id (estimation_id),
    INDEX idx_est_items_match_status (match_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
