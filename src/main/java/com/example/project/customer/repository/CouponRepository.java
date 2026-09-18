package com.example.project.customer.repository;

import com.example.project.customer.entity.Coupon;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCaseAndIsActiveTrue(String code);
    Optional<Coupon> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Coupon> findByIsActiveTrue();
    @Query("SELECT c FROM Coupon c WHERE (:active IS NULL OR c.isActive = :active) AND (:search IS NULL OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Coupon> search(@Param("active") Boolean active, @Param("search") String search, Pageable pageable);
    @Modifying @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.id = :id AND (c.totalUsageLimit IS NULL OR c.usedCount < c.totalUsageLimit)")
    int incrementUsageIfAvailable(@Param("id") Long id);
}
