-- ========================================================
-- Flyway Database Migration: V8__create_admins_table.sql
-- Dedicated Admin Users table for HinchMart Platform
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS admins (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    firebase_uid VARCHAR(128) NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(32) NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uq_admins_email (email),
    UNIQUE INDEX uq_admins_firebase_uid (firebase_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed primary Super Admin
INSERT INTO admins (name, email, phone, role, is_active)
SELECT 'HinchMart Super Admin', 'admin@hinchmart.com', '9999999999', 'ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM admins WHERE LOWER(email) = 'admin@hinchmart.com');

SET FOREIGN_KEY_CHECKS = 1;
