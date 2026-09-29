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
    Page<Order> findByCustomer_CustomerIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(Integer userId, String orderStatus, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM Order o WHERE o.customer.customerId = :userId")
    int countByUserId(@org.springframework.data.repository.query.Param("userId") Integer userId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM Order o WHERE o.customer.customerId = :userId AND UPPER(COALESCE(o.orderStatus, '')) <> 'CANCELLED'")
    long countNonCancelledByCustomerId(@org.springframework.data.repository.query.Param("userId") Integer userId);

    Page<Order> findByStore_StoreIdOrderByCreatedAtDesc(Integer storeId, Pageable pageable);

    Page<Order> findByStore_StoreIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(Integer storeId, String orderStatus, Pageable pageable);

    Page<Order> findByStore_Seller_SellerIdOrderByCreatedAtDesc(Integer sellerId, Pageable pageable);

    Page<Order> findByStore_Seller_SellerIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(Integer sellerId, String orderStatus, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Order o SET o.riderId = :riderId, o.riderName = :riderName, o.riderPhone = :riderPhone, " +
            "o.vehicleNumber = :vehicleNumber, o.orderStatus = 'RIDER_ASSIGNED', o.riderAssignedAt = :assignedAt " +
            "WHERE o.orderId = :orderId AND (o.riderId IS NULL OR o.orderStatus = 'ACCEPTED_BY_SELLER')")
    int atomicAssignRiderToOrder(@org.springframework.data.repository.query.Param("orderId") Integer orderId,
                                 @org.springframework.data.repository.query.Param("riderId") Long riderId,
                                 @org.springframework.data.repository.query.Param("riderName") String riderName,
                                 @org.springframework.data.repository.query.Param("riderPhone") String riderPhone,
                                 @org.springframework.data.repository.query.Param("vehicleNumber") String vehicleNumber,
                                 @org.springframework.data.repository.query.Param("assignedAt") java.time.LocalDateTime assignedAt);
}
