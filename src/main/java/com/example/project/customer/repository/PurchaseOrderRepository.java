package com.example.project.customer.repository;

import com.example.project.customer.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {
    Optional<PurchaseOrder> findByPoNumber(String poNumber);
    Page<PurchaseOrder> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    Page<PurchaseOrder> findByUserIdAndStatusOrderByCreatedAtDesc(Integer userId, String status, Pageable pageable);
    Page<PurchaseOrder> findByVendorIdOrderByCreatedAtDesc(Integer vendorId, Pageable pageable);
}
