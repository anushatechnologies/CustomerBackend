package com.example.project.customer.repository;

import com.example.project.customer.entity.InventoryAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    
    // Find adjustments by seller and product
    List<InventoryAdjustment> findBySellerIdAndProductId(String sellerId, String productId);
    
    // Find adjustments by seller with pagination
    Page<InventoryAdjustment> findBySellerId(String sellerId, Pageable pageable);
    
    // Find adjustments by warehouse
    List<InventoryAdjustment> findByWarehouseId(String warehouseId);
}
