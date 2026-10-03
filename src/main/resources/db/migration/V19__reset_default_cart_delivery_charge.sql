-- ====================================================================
-- Flyway Migration: V19__reset_default_cart_delivery_charge.sql
-- Removes hardcoded 4500.00 default delivery charge from carts table.
-- ====================================================================

-- 1. Alter column default to 0.00
ALTER TABLE carts ALTER COLUMN delivery_charge SET DEFAULT 0.00;

-- 2. Update existing carts that were set to the legacy hardcoded 4500.00 charge
UPDATE carts SET delivery_charge = 0.00 WHERE delivery_charge = 4500.00;
