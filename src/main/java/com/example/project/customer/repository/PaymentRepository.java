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

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Payment p SET p.status = 'REFUND_IN_PROGRESS', p.updatedAt = CURRENT_TIMESTAMP WHERE p.paymentId = :paymentId AND p.status NOT IN ('REFUND_IN_PROGRESS', 'REFUNDED', 'REFUNDED_TO_WALLET')")
    int claimRefundAtomic(@org.springframework.data.repository.query.Param("paymentId") Integer paymentId);

    List<Payment> findByStatusAndUpdatedAtBefore(String status, java.time.LocalDateTime cutoff);

    List<Payment> findByStatusOrderByUpdatedAtDesc(String status);
}
