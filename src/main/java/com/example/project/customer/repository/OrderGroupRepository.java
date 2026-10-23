package com.example.project.customer.repository;

import com.example.project.customer.entity.OrderGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderGroupRepository extends JpaRepository<OrderGroup, Integer> {

    Optional<OrderGroup> findByGroupOrderNumber(String groupOrderNumber);

    List<OrderGroup> findByCustomer_CustomerIdOrderByCreatedAtDesc(Integer customerId);

    List<OrderGroup> findByPaymentStatus(String paymentStatus);
}
