SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS add_uq_coupon_redemption_v12;

DELIMITER $$

CREATE PROCEDURE add_uq_coupon_redemption_v12()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'coupon_usages' 
          AND CONSTRAINT_NAME = 'uq_coupon_usage_redemption'
    ) THEN
        ALTER TABLE coupon_usages
            ADD CONSTRAINT uq_coupon_usage_redemption UNIQUE (coupon_id, customer_id, order_id);
    END IF;
END $$

DELIMITER ;

CALL add_uq_coupon_redemption_v12();

DROP PROCEDURE IF EXISTS add_uq_coupon_redemption_v12;

SET FOREIGN_KEY_CHECKS = 1;
