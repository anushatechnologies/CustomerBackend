-- ========================================================
-- Flyway Database Migration: V24__add_unique_constraint_to_subcategories.sql
-- Enforces uniqueness of subcategory slug and name under parent category
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS add_subcategory_unique_constraints_v24;

DELIMITER $$

CREATE PROCEDURE add_subcategory_unique_constraints_v24()
BEGIN
    -- 1. Ensure unique constraint on (category_id, slug)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE table_schema = DATABASE() 
          AND table_name = 'subcategories' 
          AND index_name = 'uk_subcategories_category_slug'
    ) THEN
        ALTER TABLE subcategories ADD CONSTRAINT uk_subcategories_category_slug UNIQUE (category_id, slug);
    END IF;

    -- 2. Ensure unique constraint on (category_id, name)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE table_schema = DATABASE() 
          AND table_name = 'subcategories' 
          AND index_name = 'uk_subcategories_category_name'
    ) THEN
        ALTER TABLE subcategories ADD CONSTRAINT uk_subcategories_category_name UNIQUE (category_id, name);
    END IF;
END $$

DELIMITER ;

CALL add_subcategory_unique_constraints_v24();
DROP PROCEDURE IF EXISTS add_subcategory_unique_constraints_v24;

SET FOREIGN_KEY_CHECKS = 1;
