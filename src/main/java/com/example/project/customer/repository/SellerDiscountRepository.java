package com.example.project.customer.repository;

import com.example.project.customer.entity.DiscountStatus;
import com.example.project.customer.entity.SellerDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerDiscountRepository extends JpaRepository<SellerDiscount, Integer> {

    Optional<SellerDiscount> findByCodeIgnoreCase(String code);

    List<SellerDiscount> findBySellerIdOrderByCreatedAtDesc(Integer sellerId);

    List<SellerDiscount> findByStatusOrderByCreatedAtAsc(DiscountStatus status);

    List<SellerDiscount> findByStatusAndActiveTrueOrderByCreatedAtDesc(DiscountStatus status);

    boolean existsByCodeIgnoreCaseAndSellerIdNot(String code, Integer sellerId);
}
