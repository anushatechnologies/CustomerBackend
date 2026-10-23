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
 
    List<Payment> findByOrderGroup_IdOrderByCreatedAtDesc(Integer orderGroupId);

    Optional<Payment> findFirstByOrderGroup_IdOrderByCreatedAtDesc(Integer orderGroupId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Payment p SET p.status = 'REFUND_IN_PROGRESS', p.updatedAt = CURRENT_TIMESTAMP WHERE p.paymentId = :paymentId AND p.status IN ('CAPTURED', 'REFUND_PENDING', 'PARTIALLY_REFUNDED')")
    int claimRefundAtomic(@org.springframework.data.repository.query.Param("paymentId") Integer paymentId);

    List<Payment> findByStatusAndUpdatedAtBefore(String status, java.time.LocalDateTime cutoff);

    List<Payment> findByStatusOrderByUpdatedAtDesc(String status);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p WHERE " +
            "((p.order IS NOT NULL AND p.order.orderStatus = 'CANCELLED' AND p.status = 'CAPTURED' AND p.order.updatedAt < :cutoff) OR " +
            "(p.orderGroup IS NOT NULL AND p.status IN ('CAPTURED', 'PARTIALLY_REFUNDED') AND EXISTS (SELECT o FROM Order o WHERE o.orderGroup = p.orderGroup AND o.orderStatus = 'CANCELLED' AND o.paymentStatus <> 'REFUNDED' AND o.updatedAt < :cutoff))) " +
            "AND p.paymentId > :lastId ORDER BY p.paymentId ASC")
    List<Payment> findCapturedPaymentsForCancelledOrdersOlderThan(
            @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff,
            @org.springframework.data.repository.query.Param("lastId") Integer lastId,
            org.springframework.data.domain.Pageable pageable);

    default List<Payment> findCapturedPaymentsForCancelledOrdersOlderThan(java.time.LocalDateTime cutoff, org.springframework.data.domain.Pageable pageable) {
        return findCapturedPaymentsForCancelledOrdersOlderThan(cutoff, 0, pageable);
    }
}
