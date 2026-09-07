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
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByCustomer_CustomerIdOrderByCreatedAtDesc(Integer userId);
    List<Order> findByCustomer_CustomerIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(Integer userId, String orderStatus);
    Page<Order> findByCustomer_CustomerIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM Order o WHERE o.customer.customerId = :userId")
    int countByUserId(@org.springframework.data.repository.query.Param("userId") Integer userId);
}
