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
    Page<PurchaseOrder> findByCustomer_CustomerIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    Page<PurchaseOrder> findByCustomer_CustomerIdAndStatusOrderByCreatedAtDesc(Integer userId, String status, Pageable pageable);
    Page<PurchaseOrder> findBySellerIdOrderByCreatedAtDesc(Integer sellerId, Pageable pageable);
}
