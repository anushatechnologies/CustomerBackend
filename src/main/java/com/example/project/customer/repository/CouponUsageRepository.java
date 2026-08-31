package com.example.project.customer.repository;

import com.example.project.customer.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    @Query("SELECT COUNT(cu) FROM CouponUsage cu WHERE cu.coupon.couponId = :couponId " +
            "AND cu.userId = :userId")
    Integer countUserCouponUsage(@Param("couponId") Integer couponId, @Param("userId") Integer userId);

    @Query("SELECT cu FROM CouponUsage cu WHERE cu.coupon.couponId = :couponId " +
            "AND cu.userId = :userId")
    List<CouponUsage> findByUserAndCoupon(@Param("couponId") Integer couponId, @Param("userId") Integer userId);

    @Query("SELECT cu FROM CouponUsage cu WHERE cu.orderId = :orderId")
    List<CouponUsage> findByOrderId(@Param("orderId") Integer orderId);
}
