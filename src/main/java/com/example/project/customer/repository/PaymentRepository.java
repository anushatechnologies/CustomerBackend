package com.example.project.customer.repository;

import com.example.project.customer.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Payment> findByCustomer_CustomerIdOrderByCreatedAtDesc(Integer customerId);

    List<Payment> findByOrder_OrderIdOrderByCreatedAtDesc(Integer orderId);

    Optional<Payment> findFirstByOrder_OrderIdOrderByCreatedAtDesc(Integer orderId);
}
