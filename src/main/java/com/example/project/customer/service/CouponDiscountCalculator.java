package com.example.project.customer.service;

import com.example.project.customer.entity.Coupon;
import com.example.project.customer.entity.DiscountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates the discount amount for a coupon based on discount type and order fare.
 */
@Component
public class CouponDiscountCalculator {

    /**
     * Calculate the discount amount for a given coupon and order fare.
     *
     * @param coupon the coupon
     * @param orderFare the order fare
     * @return the discount amount (never exceeds the order fare itself)
     */
    public BigDecimal calculate(Coupon coupon, BigDecimal orderFare) {
        if (coupon == null || orderFare == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;

        if (coupon.getDiscountType() == DiscountType.FLAT) {
            discount = coupon.getDiscountValue();
        } else { // PERCENTAGE
            discount = orderFare.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            // Apply max discount cap if set
            if (coupon.getMaxDiscountAmount() != null) {
                discount = discount.min(coupon.getMaxDiscountAmount());
            }
        }

        // Never let discount exceed the fare itself
        return discount.min(orderFare);
    }
}
