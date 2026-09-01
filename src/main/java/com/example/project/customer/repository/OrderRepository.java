package com.example.project.customer.repository;

import com.example.project.customer.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<Order> findByUserIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(Integer userId, String orderStatus);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    Page<Order> findByUserIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(Integer userId, String orderStatus, Pageable pageable);
    int countByUserId(Integer userId);
}
