package com.example.project.customer.repository;

import com.example.project.customer.entity.OrderTrackingCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTrackingCheckpoint, Integer> {
    List<OrderTrackingCheckpoint> findByOrder_OrderIdOrderBySortOrderAsc(Integer orderId);
}
