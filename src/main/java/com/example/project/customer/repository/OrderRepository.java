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
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findByIdForUpdate(@org.springframework.data.repository.query.Param("orderId") Integer orderId);

    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByOrderGroup_Id(Integer orderGroupId);
    List<Order> findByCustomer_CustomerIdOrderByCreatedAtDesc(Integer userId);
    List<Order> findByCustomer_CustomerIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(Integer userId, String orderStatus);
    Page<Order> findByCustomer_CustomerIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    Page<Order> findByCustomer_CustomerIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(Integer userId, String orderStatus, Pageable pageable);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Order> findByOrderStatusIgnoreCaseOrderByCreatedAtDesc(String orderStatus, Pageable pageable);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByOrderStatusIgnoreCaseOrderByCreatedAtDesc(String orderStatus);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM Order o WHERE o.customer.customerId = :userId")
    int countByUserId(@org.springframework.data.repository.query.Param("userId") Integer userId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM Order o WHERE o.customer.customerId = :userId AND UPPER(COALESCE(o.orderStatus, '')) <> 'CANCELLED'")
    long countNonCancelledByCustomerId(@org.springframework.data.repository.query.Param("userId") Integer userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Order o SET o.inventoryState = :newState WHERE o.orderId = :orderId AND o.inventoryState = :expectedState")
    int transitionInventoryState(@org.springframework.data.repository.query.Param("orderId") Integer orderId,
                                 @org.springframework.data.repository.query.Param("expectedState") com.example.project.customer.entity.OrderInventoryState expectedState,
                                 @org.springframework.data.repository.query.Param("newState") com.example.project.customer.entity.OrderInventoryState newState);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT DISTINCT o FROM Order o " +
                    "LEFT JOIN o.items oi " +
                    "LEFT JOIN Product p ON p.productId = oi.productId " +
                    "WHERE (o.fulfillmentStore.seller.sellerId = :sellerId OR " +
                    "       o.store.seller.sellerId = :sellerId OR " +
                    "       oi.seller.sellerId = :sellerId OR " +
                    "       p.seller.sellerId = :sellerId) " +
                    "ORDER BY o.createdAt DESC",
            countQuery = "SELECT COUNT(DISTINCT o) FROM Order o " +
                    "LEFT JOIN o.items oi " +
                    "LEFT JOIN Product p ON p.productId = oi.productId " +
                    "WHERE (o.fulfillmentStore.seller.sellerId = :sellerId OR " +
                    "       o.store.seller.sellerId = :sellerId OR " +
                    "       oi.seller.sellerId = :sellerId OR " +
                    "       p.seller.sellerId = :sellerId)")
    Page<Order> findSellerOrders(@org.springframework.data.repository.query.Param("sellerId") Integer sellerId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT DISTINCT o FROM Order o " +
                    "LEFT JOIN o.items oi " +
                    "LEFT JOIN Product p ON p.productId = oi.productId " +
                    "WHERE (o.fulfillmentStore.seller.sellerId = :sellerId OR " +
                    "       o.store.seller.sellerId = :sellerId OR " +
                    "       oi.seller.sellerId = :sellerId OR " +
                    "       p.seller.sellerId = :sellerId) AND " +
                    "UPPER(o.orderStatus) = UPPER(:orderStatus) " +
                    "ORDER BY o.createdAt DESC",
            countQuery = "SELECT COUNT(DISTINCT o) FROM Order o " +
                    "LEFT JOIN o.items oi " +
                    "LEFT JOIN Product p ON p.productId = oi.productId " +
                    "WHERE (o.fulfillmentStore.seller.sellerId = :sellerId OR " +
                    "       o.store.seller.sellerId = :sellerId OR " +
                    "       oi.seller.sellerId = :sellerId OR " +
                    "       p.seller.sellerId = :sellerId) AND " +
                    "UPPER(o.orderStatus) = UPPER(:orderStatus)")
    Page<Order> findSellerOrdersByStatus(@org.springframework.data.repository.query.Param("sellerId") Integer sellerId,
                                         @org.springframework.data.repository.query.Param("orderStatus") String orderStatus,
                                         Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.store.storeId = :platformStoreId AND UPPER(o.orderStatus) = 'PLACED' AND UPPER(o.paymentStatus) = 'PENDING' AND o.inventoryState = com.example.project.customer.entity.OrderInventoryState.RESERVED AND o.createdAt < :cutoff")
    List<Order> findExpiredUnpaidHinchOrders(@org.springframework.data.repository.query.Param("platformStoreId") Integer platformStoreId,
                                            @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);
}
