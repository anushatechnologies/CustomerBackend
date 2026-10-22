-- Flyway Database Migration: V30__reconcile_hinch_store_vendor_inventory.sql
-- Safely backfill missing hinch_store_vendor_inventory records for existing HinchStore products with positive stock.
-- Idempotent, non-destructive, and strictly isolates independent seller inventory.

INSERT INTO hinch_store_vendor_inventory (product_id, vendor_store_id, qty, reserved_qty, is_active, created_at, updated_at)
SELECT p.product_id,
       COALESCE(s.store_id, p.store_id, 1) AS vendor_store_id,
       p.stock_qty,
       0,
       TRUE,
       NOW(),
       NOW()
FROM products p
LEFT JOIN stores s ON s.seller_id = p.seller_id
WHERE p.store_id = 1
  AND p.stock_qty > 0
  AND NOT EXISTS (
      SELECT 1 FROM hinch_store_vendor_inventory h
      WHERE h.product_id = p.product_id
  );
