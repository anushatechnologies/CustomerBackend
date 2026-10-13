-- Flyway Database Migration: V26__commission_rules_and_item_settlement.sql
-- Commission rules table, OrderItem settlement snapshots, and PayoutLedger item-level tracking

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Commission Rules Table
CREATE TABLE IF NOT EXISTS commission_rules (
    rule_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NULL,
    rate DECIMAL(5, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    effective_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_comm_rules_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL,
    INDEX idx_comm_rules_lookup (category_id, is_active, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default global HinchStore holding fee rule as INACTIVE per business requirements
INSERT INTO commission_rules (category_id, rate, is_active)
VALUES (NULL, 5.00, FALSE);

-- 2. Add settlement snapshots to order_items (No cost/base column added)
ALTER TABLE order_items
    ADD COLUMN seller_id INT NULL AFTER product_id,
    ADD COLUMN fulfillment_store_id INT NULL AFTER seller_id,
    ADD COLUMN is_hinch_store BOOLEAN NOT NULL DEFAULT FALSE AFTER fulfillment_store_id,
    ADD COLUMN commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00 AFTER line_gst,
    ADD COLUMN seller_payable DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER commission_rate,
    ADD COLUMN hinch_keeps DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER seller_payable,
    ADD CONSTRAINT fk_oi_seller FOREIGN KEY (seller_id) REFERENCES sellers(seller_id),
    ADD CONSTRAINT fk_oi_fulfillment_store FOREIGN KEY (fulfillment_store_id) REFERENCES stores(store_id);

-- 3. Add item-level, seller and hinch_keeps tracking to seller_payout_ledgers
ALTER TABLE seller_payout_ledgers
    ADD COLUMN seller_id INT NULL AFTER store_id,
    ADD COLUMN order_item_id INT NULL AFTER order_id,
    ADD COLUMN hinch_keeps DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER net_payout_amount,
    ADD CONSTRAINT fk_spl_seller FOREIGN KEY (seller_id) REFERENCES sellers(seller_id),
    ADD CONSTRAINT fk_spl_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id) ON DELETE SET NULL;

SET FOREIGN_KEY_CHECKS = 1;
