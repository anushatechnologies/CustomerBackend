-- ========================================================
-- Flyway Database Migration: V23__widen_order_recipient_phone.sql
-- Widen recipient_phone column in orders table to VARCHAR(50)
-- ========================================================

DROP PROCEDURE IF EXISTS upgrade_orders_recipient_phone_v23;

DELIMITER $$

CREATE PROCEDURE upgrade_orders_recipient_phone_v23()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'recipient_phone'
    ) THEN
        ALTER TABLE orders MODIFY COLUMN recipient_phone VARCHAR(50) NULL;
    END IF;
END $$

DELIMITER ;

CALL upgrade_orders_recipient_phone_v23();
DROP PROCEDURE IF EXISTS upgrade_orders_recipient_phone_v23;
