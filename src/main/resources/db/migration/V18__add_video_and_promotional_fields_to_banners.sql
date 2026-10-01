-- ========================================================
-- Flyway Database Migration: V18__add_video_and_promotional_fields_to_banners.sql
-- Add promotional video, poster, badge, CTA text, and updated_at to banners table
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS upgrade_banners_v18;

DELIMITER $$

CREATE PROCEDURE upgrade_banners_v18()
BEGIN
    -- 1. Add video_url column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'banners' AND column_name = 'video_url'
    ) THEN
        ALTER TABLE banners ADD COLUMN video_url VARCHAR(1024) NULL AFTER image_url;
    END IF;

    -- 2. Add poster_url column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'banners' AND column_name = 'poster_url'
    ) THEN
        ALTER TABLE banners ADD COLUMN poster_url VARCHAR(1024) NULL AFTER video_url;
    END IF;

    -- 3. Add badge column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'banners' AND column_name = 'badge'
    ) THEN
        ALTER TABLE banners ADD COLUMN badge VARCHAR(100) NULL DEFAULT '24-HOUR DISPATCH' AFTER poster_url;
    END IF;

    -- 4. Add cta_text column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'banners' AND column_name = 'cta_text'
    ) THEN
        ALTER TABLE banners ADD COLUMN cta_text VARCHAR(100) NULL DEFAULT 'Explore 24H Catalog' AFTER badge;
    END IF;

    -- 5. Add updated_at column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'banners' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE banners ADD COLUMN updated_at DATETIME NULL AFTER created_at;
    END IF;
END $$

DELIMITER ;

CALL upgrade_banners_v18();

DROP PROCEDURE IF EXISTS upgrade_banners_v18;

SET FOREIGN_KEY_CHECKS = 1;
