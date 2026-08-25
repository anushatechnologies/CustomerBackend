package com.example.project.customer.repository;

import com.example.project.customer.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Page<Order> findByUserId(Integer userId, Pageable pageable);
    Page<Order> findByUserIdAndOrderStatusIgnoreCase(Integer userId, String orderStatus, Pageable pageable);
    Optional<Order> findByOrderIdAndUserId(Integer orderId, Integer userId);
    Optional<Order> findByOrderNumber(String orderNumber);
    long countByUserId(Integer userId);
}
