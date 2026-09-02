package com.example.project.customer.repository;

import com.example.project.customer.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    List<Warehouse> findBySellerId(Integer sellerId);
    Optional<Warehouse> findByWarehouseIdAndSellerId(Integer warehouseId, Integer sellerId);
}
