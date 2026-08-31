package com.example.project.customer.repository;

import com.example.project.customer.entity.Coupon;
import com.example.project.customer.entity.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer>, JpaSpecificationExecutor<Coupon> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    List<Coupon> findByStatus(CouponStatus status);

    List<Coupon> findByStatusAndValidFromLessThanEqualAndValidToGreaterThanEqual(
            CouponStatus status,
            LocalDateTime now1,
            LocalDateTime now2
    );

    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' " +
            "AND c.validTo < :now")
    List<Coupon> findExpiredCoupons(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.usageLimitTotal IS NOT NULL " +
            "AND c.currentUsageCount >= c.usageLimitTotal " +
            "AND c.status = 'ACTIVE'")
    List<Coupon> findExhaustedCoupons();
}
