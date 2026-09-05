package com.example.project.customer.repository;

import com.example.project.customer.entity.RewardVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardVoucherRepository extends JpaRepository<RewardVoucher, Integer> {
    Optional<RewardVoucher> findByCodeIgnoreCase(String code);

    @Query("SELECT r FROM RewardVoucher r WHERE (r.userId = :userId OR r.userId IS NULL) AND r.active = true AND r.redeemed = false ORDER BY r.createdAt DESC")
    List<RewardVoucher> findAvailableForUser(@Param("userId") Integer userId);
}
