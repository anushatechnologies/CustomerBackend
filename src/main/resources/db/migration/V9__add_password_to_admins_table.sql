-- ========================================================
-- Flyway Database Migration: V9__add_password_to_admins_table.sql
-- Add password column to admins table
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Ensure password column exists on admins table safely
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admins'
      AND COLUMN_NAME = 'password'
);

SET @sql = IF(@col_exists = 0, 'ALTER TABLE admins ADD COLUMN password VARCHAR(255) NULL AFTER phone;', 'SELECT "password column already exists in admins";');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Update password for default admin if empty
UPDATE admins
SET password = 'Admin@12345'
WHERE email = 'admin@hinchmart.com' AND (password IS NULL OR password = '' OR password = 'admin');

SET FOREIGN_KEY_CHECKS = 1;
