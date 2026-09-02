package com.example.project.customer.repository;

import com.example.project.customer.entity.InventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    List<InventoryAdjustment> findBySellerIdOrderByCreatedAtDesc(Integer sellerId);
    List<InventoryAdjustment> findByProductIdOrderByCreatedAtDesc(Integer productId);
}
