-- ========================================================
-- Flyway Database Migration: V20__add_recipient_details_to_orders.sql
-- Adds recipient contact support for "Order for someone else"
-- ========================================================

DROP PROCEDURE IF EXISTS upgrade_orders_recipient_v20;

DELIMITER $$

CREATE PROCEDURE upgrade_orders_recipient_v20()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'order_for_someone_else'
    ) THEN
        ALTER TABLE orders ADD COLUMN order_for_someone_else BOOLEAN DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'recipient_name'
    ) THEN
        ALTER TABLE orders ADD COLUMN recipient_name VARCHAR(150) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'recipient_phone'
    ) THEN
        ALTER TABLE orders ADD COLUMN recipient_phone VARCHAR(20) NULL;
    END IF;
END $$

DELIMITER ;

CALL upgrade_orders_recipient_v20();
DROP PROCEDURE IF EXISTS upgrade_orders_recipient_v20;
