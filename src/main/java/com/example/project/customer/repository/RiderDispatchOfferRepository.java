package com.example.project.customer.repository;

import com.example.project.customer.entity.RiderDispatchOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiderDispatchOfferRepository extends JpaRepository<RiderDispatchOffer, Long> {

    List<RiderDispatchOffer> findByOrder_OrderIdOrderBySequenceIndexAsc(Integer orderId);

    Optional<RiderDispatchOffer> findFirstByOrder_OrderIdAndStatus(Integer orderId, String status);

    Optional<RiderDispatchOffer> findFirstByRider_IdAndStatusOrderByOfferedAtDesc(Long riderId, String status);

    List<RiderDispatchOffer> findByStatusAndExpiresAtBefore(String status, LocalDateTime now);

    @Modifying
    @Query("UPDATE RiderDispatchOffer o SET o.status = 'CANCELLED', o.respondedAt = :now WHERE o.order.orderId = :orderId AND o.status = 'OFFERED'")
    int cancelPendingOffersForOrder(@Param("orderId") Integer orderId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RiderDispatchOffer o SET o.status = 'EXPIRED', o.respondedAt = :now WHERE o.order.orderId = :orderId AND o.rider.id = :riderId AND o.status = 'OFFERED'")
    int expireOfferForRider(@Param("orderId") Integer orderId, @Param("riderId") Long riderId, @Param("now") LocalDateTime now);
}
