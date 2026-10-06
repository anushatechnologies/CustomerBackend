-- ========================================================
-- Flyway Database Migration: V21__create_hot_deals_table.sql
-- Creates Hot Deals curated product collection table
-- ========================================================

CREATE TABLE IF NOT EXISTS hot_deals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_hot_deals_product UNIQUE (product_id),
    CONSTRAINT fk_hot_deals_product FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_hot_deals_active_order ON hot_deals (is_active, display_order);
