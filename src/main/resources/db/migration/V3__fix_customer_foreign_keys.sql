-- ========================================================
-- Flyway Migration: V3__fix_customer_foreign_keys.sql
-- Resolves orphaned customer references and enforces FK constraints.
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Ensure the default customer 101 exists in customers table
-- This preserves the 3 orders, 1 cart, and the ₹75,000 wallet balance!
INSERT INTO customers (customer_id, name, email, phone, role, is_active, created_at, updated_at)
VALUES (101, 'Primary Customer', 'buyer@hinchmart.com', '9876543210', 'BUYER', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE customer_id = customer_id;

-- 2. Handle user_addresses where customer_id = 0 (reassign to customer 1 Pavan Kumar)
UPDATE user_addresses SET customer_id = 1 WHERE customer_id = 0 OR customer_id NOT IN (SELECT customer_id FROM customers);

-- 3. Delete any truly orphaned wallet rows that still have no customer
DELETE FROM wallet_transactions 
WHERE wallet_id IN (
    SELECT w.wallet_id FROM (
        SELECT wallet_id FROM wallets 
        WHERE customer_id NOT IN (SELECT customer_id FROM customers)
    ) w
);

DELETE FROM wallets 
WHERE customer_id NOT IN (SELECT customer_id FROM customers);

-- 4. Clean up any remaining orphaned carts or orders (safety check)
UPDATE carts SET customer_id = 101 WHERE customer_id IS NOT NULL AND customer_id NOT IN (SELECT customer_id FROM customers);
UPDATE orders SET customer_id = 101 WHERE customer_id IS NOT NULL AND customer_id NOT IN (SELECT customer_id FROM customers);

-- 5. Explicitly add the foreign key constraint on wallets if not present
DROP PROCEDURE IF EXISTS add_wallet_fk;

DELIMITER $$

CREATE PROCEDURE add_wallet_fk()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'wallets' 
          AND CONSTRAINT_NAME = 'FKb9yxg2wtrb2wpff51lp6gik7h'
    ) THEN
        ALTER TABLE wallets 
            ADD CONSTRAINT FKb9yxg2wtrb2wpff51lp6gik7h 
            FOREIGN KEY (customer_id) REFERENCES customers (customer_id) 
            ON DELETE CASCADE;
    END IF;
END $$

DELIMITER ;

CALL add_wallet_fk();

DROP PROCEDURE IF EXISTS add_wallet_fk;

SET FOREIGN_KEY_CHECKS = 1;
