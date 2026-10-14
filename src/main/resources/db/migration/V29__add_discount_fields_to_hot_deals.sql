-- ========================================================
-- Flyway Database Migration: V29__add_discount_fields_to_hot_deals.sql
-- Adds flexible admin-configured discount fields to hot_deals
-- and cleans up historical synthetic 1.15x MRPs on products
-- ========================================================

ALTER TABLE hot_deals
    ADD COLUMN discount_percentage DECIMAL(5, 2) NULL AFTER display_order,
    ADD COLUMN deal_price DECIMAL(12, 2) NULL AFTER discount_percentage;

-- Reset synthetic 1.15x auto-calculated MRPs to NULL so misleading 13% badge is removed
UPDATE products 
SET mrp = NULL 
WHERE mrp IS NOT NULL 
  AND price IS NOT NULL 
  AND price > 0 
  AND ROUND(mrp / price, 2) = 1.15;
