-- ========================================================
-- Flyway Database Migration: V5__add_firebase_uid_to_customers.sql
-- Add firebase_uid to customers table for Firebase Authentication
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS upgrade_customers_v5;

DELIMITER $$

CREATE PROCEDURE upgrade_customers_v5()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'customers' AND column_name = 'firebase_uid'
    ) THEN
        ALTER TABLE customers ADD COLUMN firebase_uid VARCHAR(128) NULL AFTER customer_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE table_schema = DATABASE() AND table_name = 'customers' AND index_name = 'idx_customers_firebase_uid'
    ) THEN
        ALTER TABLE customers ADD UNIQUE INDEX idx_customers_firebase_uid (firebase_uid);
    END IF;
END $$

DELIMITER ;

CALL upgrade_customers_v5();

DROP PROCEDURE IF EXISTS upgrade_customers_v5;

SET FOREIGN_KEY_CHECKS = 1;
