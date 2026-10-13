package com.example.project.customer.repository;

import com.example.project.customer.entity.CommissionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Integer> {

    @Query("SELECT r FROM CommissionRule r WHERE r.isActive = true " +
           "AND r.effectiveFrom <= :now " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :now) " +
           "AND (r.category.categoryId = :categoryId OR r.category IS NULL) " +
           "ORDER BY CASE WHEN r.category IS NOT NULL THEN 0 ELSE 1 END ASC, r.effectiveFrom DESC")
    List<CommissionRule> findApplicableRules(
            @Param("categoryId") Integer categoryId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT r FROM CommissionRule r WHERE r.isActive = true " +
           "AND r.category IS NULL " +
           "AND r.effectiveFrom <= :now " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :now)")
    List<CommissionRule> findActiveGlobalRules(@Param("now") LocalDateTime now);
}
