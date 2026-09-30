-- ========================================================
-- Flyway Database Migration: V15__make_customer_personal_fields_nullable.sql
-- Allow NULL for customer name, email, and phone for Firebase JIT registration
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS upgrade_customers_v15;

DELIMITER $$

CREATE PROCEDURE upgrade_customers_v15()
BEGIN
    -- 1. Modify name column to be NULL
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'customers' AND column_name = 'name'
    ) THEN
        ALTER TABLE customers MODIFY COLUMN name VARCHAR(255) NULL;
    END IF;

    -- 2. Modify email column to be NULL
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'customers' AND column_name = 'email'
    ) THEN
        ALTER TABLE customers MODIFY COLUMN email VARCHAR(255) NULL;
    END IF;

    -- 3. Modify phone column to be NULL
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'customers' AND column_name = 'phone'
    ) THEN
        ALTER TABLE customers MODIFY COLUMN phone VARCHAR(255) NULL;
    END IF;
END $$

DELIMITER ;

CALL upgrade_customers_v15();

DROP PROCEDURE IF EXISTS upgrade_customers_v15;

SET FOREIGN_KEY_CHECKS = 1;
