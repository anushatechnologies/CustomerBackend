SET FOREIGN_KEY_CHECKS = 0;

-- Drop legacy coupon tables if present
DROP TABLE IF EXISTS coupon_usages;
DROP TABLE IF EXISTS coupon_usage;
DROP TABLE IF EXISTS coupons;

CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    max_discount_amount DECIMAL(10,2) NULL,
    start_date DATETIME NULL,
    expiry_date DATETIME NULL,
    total_usage_limit INT NULL,
    used_count INT NOT NULL DEFAULT 0,
    per_user_limit INT NOT NULL DEFAULT 1,
    first_order_only BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    CONSTRAINT uk_coupons_code UNIQUE (code),
    INDEX idx_coupons_active_dates (is_active, start_date, expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE coupon_usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    customer_id INT NOT NULL,
    order_id INT NOT NULL,
    discount_applied DECIMAL(10,2) NOT NULL,
    used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_coupon_usages_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT fk_coupon_usages_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CONSTRAINT fk_coupon_usages_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    INDEX idx_coupon_usages_coupon (coupon_id),
    INDEX idx_coupon_usages_coupon_customer (coupon_id, customer_id),
    INDEX idx_coupon_usages_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
