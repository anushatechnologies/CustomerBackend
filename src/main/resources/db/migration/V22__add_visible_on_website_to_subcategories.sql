-- ========================================================
-- Flyway Database Migration: V22__add_visible_on_website_to_subcategories.sql
-- Adds website visibility toggle for subcategories
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS upgrade_subcategories_v22;

DELIMITER $$

CREATE PROCEDURE upgrade_subcategories_v22()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'subcategories' AND column_name = 'visible_on_website'
    ) THEN
        ALTER TABLE subcategories ADD COLUMN visible_on_website BOOLEAN NOT NULL DEFAULT TRUE AFTER is_active;
        UPDATE subcategories SET visible_on_website = TRUE WHERE visible_on_website IS NULL;
    END IF;
END $$

DELIMITER ;

CALL upgrade_subcategories_v22();

DROP PROCEDURE IF EXISTS upgrade_subcategories_v22;

SET FOREIGN_KEY_CHECKS = 1;
