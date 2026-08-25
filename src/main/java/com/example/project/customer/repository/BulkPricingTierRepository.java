package com.example.project.customer.repository;

import com.example.project.customer.entity.BulkPricingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkPricingTierRepository extends JpaRepository<BulkPricingTier, Integer> {
    List<BulkPricingTier> findByProduct_ProductIdOrderByMinQtyAsc(Integer productId);
}
