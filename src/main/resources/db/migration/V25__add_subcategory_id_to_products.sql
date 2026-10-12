-- ========================================================
-- Flyway Database Migration: V25__add_subcategory_id_to_products.sql
-- Ensures nullable subcategory_id column exists on products table with foreign key
-- Idempotent check ensures safety on databases where column was already added
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS add_product_subcategory_column_v25;

DELIMITER $$

CREATE PROCEDURE add_product_subcategory_column_v25()
BEGIN
    -- 1. Ensure subcategory_id column exists on products
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() 
          AND table_name = 'products' 
          AND column_name = 'subcategory_id'
    ) THEN
        ALTER TABLE products ADD COLUMN subcategory_id INT NULL AFTER brand_id;
    END IF;

    -- 2. Ensure foreign key constraint fk_products_subcategory exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE table_schema = DATABASE() 
          AND table_name = 'products' 
          AND constraint_name = 'fk_products_subcategory'
    ) THEN
        ALTER TABLE products ADD CONSTRAINT fk_products_subcategory 
            FOREIGN KEY (subcategory_id) REFERENCES subcategories (subcategory_id) ON DELETE SET NULL;
    END IF;
END $$

DELIMITER ;

CALL add_product_subcategory_column_v25();
DROP PROCEDURE IF EXISTS add_product_subcategory_column_v25;

SET FOREIGN_KEY_CHECKS = 1;
