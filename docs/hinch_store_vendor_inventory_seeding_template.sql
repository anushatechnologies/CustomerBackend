-- =============================================================================
-- SQL Seeding Template: hinch_store_vendor_inventory
-- =============================================================================
-- Purpose: Seed vendor consignment inventory for HinchStore platform products (store_id = 1).
--
-- Instructions:
-- 1. Replace <VENDOR_STORE_ID> with the actual vendor store ID (must NOT be 1).
-- 2. Replace <PRODUCT_ID_*> with the product IDs belonging to HinchStore.
-- 3. Replace <QTY_*> with the initial available quantity.
-- 4. Execute manually against the production/staging database when ready.
-- DO NOT RUN AUTOMATICALLY AS A FLYWAY MIGRATION.
-- =============================================================================

INSERT INTO hinch_store_vendor_inventory (
    product_id,
    vendor_store_id,
    qty,
    reserved_qty,
    is_active,
    created_at,
    updated_at
) VALUES
    (<PRODUCT_ID_1>, <VENDOR_STORE_ID>, <QTY_1>, 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (<PRODUCT_ID_2>, <VENDOR_STORE_ID>, <QTY_2>, 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (<PRODUCT_ID_3>, <VENDOR_STORE_ID>, <QTY_3>, 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    qty = VALUES(qty),
    is_active = VALUES(is_active),
    updated_at = CURRENT_TIMESTAMP;

-- Verification Query:
-- SELECT i.id, i.product_id, p.title, i.vendor_store_id, s.name AS vendor_store_name,
--        i.qty, i.reserved_qty, (i.qty - i.reserved_qty) AS available_qty, i.is_active
-- FROM hinch_store_vendor_inventory i
-- JOIN products p ON p.product_id = i.product_id
-- JOIN stores s ON s.store_id = i.vendor_store_id
-- WHERE i.vendor_store_id = <VENDOR_STORE_ID>;
