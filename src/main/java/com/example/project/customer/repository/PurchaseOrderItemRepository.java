package com.example.project.customer.repository;

import com.example.project.customer.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Integer> {
    List<PurchaseOrderItem> findByPurchaseOrder_PoId(Integer poId);
}
