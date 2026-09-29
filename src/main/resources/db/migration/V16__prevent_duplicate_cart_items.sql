-- ========================================================
-- Flyway Database Migration: V16__prevent_duplicate_cart_items.sql
-- Enforces uniqueness on (cart_id, product_id) in cart_items table
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Deduplicate any existing duplicate items in the cart_items table
DELETE c1 FROM cart_items c1
INNER JOIN cart_items c2 
WHERE c1.cart_item_id < c2.cart_item_id 
  AND c1.cart_id = c2.cart_id 
  AND c1.product_id = c2.product_id;

-- 2. Add Unique Constraint on (cart_id, product_id)
DROP PROCEDURE IF EXISTS add_uq_cart_product_v16;

DELIMITER $$

CREATE PROCEDURE add_uq_cart_product_v16()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'cart_items' 
          AND CONSTRAINT_NAME = 'uk_cart_product'
    ) THEN
        ALTER TABLE cart_items
            ADD CONSTRAINT uk_cart_product UNIQUE (cart_id, product_id);
    END IF;
END $$

DELIMITER ;

CALL add_uq_cart_product_v16();

DROP PROCEDURE IF EXISTS add_uq_cart_product_v16;

SET FOREIGN_KEY_CHECKS = 1;
