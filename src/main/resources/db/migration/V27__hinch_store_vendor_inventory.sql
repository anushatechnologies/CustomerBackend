-- Flyway Database Migration: V27__hinch_store_vendor_inventory.sql
-- HinchStore consignment inventory, fulfillment store tracking, and order inventory state

-- 1. HinchStore Consignment Inventory Table
CREATE TABLE IF NOT EXISTS hinch_store_vendor_inventory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    vendor_store_id INT NOT NULL,
    qty INT NOT NULL DEFAULT 0,
    reserved_qty INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_hsvi_product_store UNIQUE (product_id, vendor_store_id),
    CONSTRAINT fk_hsvi_product FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_hsvi_vendor_store FOREIGN KEY (vendor_store_id) REFERENCES stores (store_id) ON DELETE CASCADE,
    CONSTRAINT chk_hsvi_qty CHECK (qty >= 0),
    CONSTRAINT chk_hsvi_reserved CHECK (reserved_qty >= 0),
    INDEX idx_hsvi_product_lookup (product_id, is_active),
    INDEX idx_hsvi_store_lookup (vendor_store_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Add fulfillment_store_id and inventory_state to orders
ALTER TABLE orders
    ADD COLUMN fulfillment_store_id INT NULL AFTER store_id,
    ADD COLUMN inventory_state VARCHAR(50) NOT NULL DEFAULT 'NONE' AFTER commission_amount,
    ADD CONSTRAINT fk_orders_fulfillment_store FOREIGN KEY (fulfillment_store_id) REFERENCES stores (store_id);
