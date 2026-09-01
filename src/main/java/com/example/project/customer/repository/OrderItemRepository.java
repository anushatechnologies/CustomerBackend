package com.example.project.customer.repository;

import com.example.project.customer.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrder_OrderId(Integer orderId);
}
