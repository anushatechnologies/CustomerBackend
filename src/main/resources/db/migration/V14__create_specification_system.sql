-- ========================================================
-- Flyway Database Migration: V14__create_specification_system.sql
-- Creates Specification Master, Options, and Category Specification Mappings
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Specifications Master Table
CREATE TABLE IF NOT EXISTS specifications (
    specification_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    spec_key VARCHAR(100) NOT NULL,
    input_type VARCHAR(50) NOT NULL,
    unit VARCHAR(50) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    CONSTRAINT uk_specifications_key UNIQUE (spec_key),
    INDEX idx_specifications_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Specification Options Table
CREATE TABLE IF NOT EXISTS specification_options (
    option_id INT AUTO_INCREMENT PRIMARY KEY,
    specification_id INT NOT NULL,
    option_value VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_spec_options_spec FOREIGN KEY (specification_id) REFERENCES specifications (specification_id) ON DELETE CASCADE,
    INDEX idx_spec_options_spec_id (specification_id),
    INDEX idx_spec_options_order (specification_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Category Specification Mapping Table
CREATE TABLE IF NOT EXISTS category_specifications (
    category_spec_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    specification_id INT NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    CONSTRAINT fk_cat_spec_category FOREIGN KEY (category_id) REFERENCES categories (category_id) ON DELETE CASCADE,
    CONSTRAINT fk_cat_spec_specification FOREIGN KEY (specification_id) REFERENCES specifications (specification_id) ON DELETE CASCADE,
    CONSTRAINT uk_category_specification UNIQUE (category_id, specification_id),
    INDEX idx_cat_spec_category_id (category_id),
    INDEX idx_cat_spec_specification_id (specification_id),
    INDEX idx_cat_spec_order (category_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
