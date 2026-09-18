package com.example.project.customer.repository;

import com.example.project.customer.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCoupon(Coupon coupon);
    long countByCouponAndCustomer(Coupon coupon, Customer customer);
    Page<CouponUsage> findByCouponOrderByUsedAtDesc(Coupon coupon, Pageable pageable);
}
