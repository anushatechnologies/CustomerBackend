package com.example.project.customer.repository;

import com.example.project.customer.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
    
    // Find all warehouses for a seller
    List<Warehouse> findBySellerId(String sellerId);
    
    // Find warehouse by ID and seller ID (ownership check)
    Optional<Warehouse> findByIdAndSellerId(String id, String sellerId);
    
    // Find default warehouse for a seller
    Optional<Warehouse> findBySellerIdAndIsDefaultTrue(String sellerId);
}
