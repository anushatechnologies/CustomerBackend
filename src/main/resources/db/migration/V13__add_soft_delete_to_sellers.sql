-- ========================================================
-- Flyway Database Migration: V13__add_soft_delete_to_sellers.sql
-- Add soft-delete fields (is_deleted, deleted_at) to sellers table
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS upgrade_sellers_soft_delete_v13;

DELIMITER $$

CREATE PROCEDURE upgrade_sellers_soft_delete_v13()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'sellers' AND column_name = 'is_deleted'
    ) THEN
        ALTER TABLE sellers ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER verification_status;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'sellers' AND column_name = 'deleted_at'
    ) THEN
        ALTER TABLE sellers ADD COLUMN deleted_at DATETIME NULL AFTER is_deleted;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE table_schema = DATABASE() AND table_name = 'sellers' AND index_name = 'idx_sellers_is_deleted'
    ) THEN
        ALTER TABLE sellers ADD INDEX idx_sellers_is_deleted (is_deleted);
    END IF;
END $$

DELIMITER ;

CALL upgrade_sellers_soft_delete_v13();

DROP PROCEDURE IF EXISTS upgrade_sellers_soft_delete_v13;

SET FOREIGN_KEY_CHECKS = 1;
